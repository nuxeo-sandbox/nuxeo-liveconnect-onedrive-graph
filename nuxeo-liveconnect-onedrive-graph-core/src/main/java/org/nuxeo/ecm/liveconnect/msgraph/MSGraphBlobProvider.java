/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc
 *     Michael Vachette
 */
package org.nuxeo.ecm.liveconnect.msgraph;

import com.google.api.client.auth.oauth2.Credential;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Permission;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class MSGraphBlobProvider extends AbstractLiveConnectBlobProvider<MSGraphOAuth2ServiceProvider> {

    private static final Log log = LogFactory.getLog(MSGraphBlobProvider.class);

    private static final String CACHE_NAME = "msgraph";

    private static final String ONEDRIVE_DOCUMENT_TO_BE_UPDATED_PP = "onedrive_document_to_be_updated";

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected String getPageProviderNameForUpdate() {
        return ONEDRIVE_DOCUMENT_TO_BE_UPDATED_PP;
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        String url = null;
        switch (usage) {
        case STREAM:
        case DOWNLOAD:
            url = GetDriveItem(fileInfo).webUrl;
            break;
        case VIEW:
        case EDIT:
            url = getSharableLink(fileInfo);
            break;
        }
        return url == null ? null : asURI(url);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        IGraphServiceClient graphServiceClient = getGraphClient(fileInfo);
        return graphServiceClient.me().drive().items(fileInfo.getFileId()).content().buildRequest().get();
    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) throws IOException {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        IGraphServiceClient graphServiceClient = getGraphClient(fileInfo);
        return graphServiceClient.me().drive().items(fileInfo.getFileId()).thumbnails("0").
                getThumbnailSize("large").content().buildRequest().get();
    }

    @Override
    public List<AppLink> getAppLinks(String username, ManagedBlob blob) throws IOException {
        // application links do not work with document which are not office document
        if (!blob.getMimeType().contains("officedocument")) {
            return Collections.emptyList();
        }
        // application links do not work with revisions
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        if (fileInfo.getRevisionId().isPresent()) {
            return Collections.emptyList();
        }
        String baseUrl = Framework.getProperty("nuxeo.url", VirtualHostHelper.getContextPathProperty());
        AppLink appLink = new AppLink();
        String appUrl = getSharableLink(fileInfo);
        appLink.setLink(appUrl);
        appLink.setAppName("Microsoft OneDrive");
        appLink.setIcon(baseUrl + "/icons/OneDrive.png");
        return Collections.singletonList(appLink);
    }

    protected IGraphServiceClient getGraphClient(LiveConnectFileInfo fileInfo) throws IOException {
        return getGraphClient(getCredential(fileInfo));
    }

    protected IGraphServiceClient getGraphClient(Credential credential) {
        return getOAuth2Provider().getGraphClient(credential);
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return new MSGraphDriveItemLiveConnectFile(fileInfo, retrieveOneDriveFileMetadata(fileInfo));
    }

    protected DriveItem retrieveOneDriveFileMetadata(LiveConnectFileInfo fileInfo) throws IOException {
        return GetDriveItem(fileInfo);
    }

    protected DriveItem GetDriveItem(LiveConnectFileInfo fileInfo) throws IOException {
        IGraphServiceClient graphClient = getGraphClient(fileInfo);
        return graphClient.me().drive().items(fileInfo.getFileId()).buildRequest().get();
    }

    protected String getSharableLink(LiveConnectFileInfo fileInfo) throws IOException {
        IGraphServiceClient graphClient = getGraphClient(fileInfo);
        Permission permission = graphClient.me().drive().items(fileInfo.getFileId())
                .createLink("edit","anonymous",null,null,null)
                .buildRequest()
                .post();
        return permission.link.webUrl;
    }

}
