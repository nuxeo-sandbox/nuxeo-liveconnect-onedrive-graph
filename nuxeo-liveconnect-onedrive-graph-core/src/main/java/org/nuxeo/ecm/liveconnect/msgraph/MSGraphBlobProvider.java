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
import com.microsoft.graph.drives.item.items.item.DriveItemItemRequestBuilder;
import com.microsoft.graph.drives.item.items.item.createlink.CreateLinkPostRequestBody;
import com.microsoft.graph.drives.item.items.item.preview.PreviewPostRequestBody;
import com.microsoft.graph.models.*;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectBlobProvider;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class MSGraphBlobProvider extends AbstractLiveConnectBlobProvider<MSGraphOAuth2ServiceProvider> {

    private static final Logger log = LogManager.getLogger(MSGraphBlobProvider.class);

    private static final String CACHE_NAME = "msgraph";

    private static final String ONEDRIVE_DOCUMENT_TO_BE_UPDATED_PP = "msgraph_document_to_be_updated";

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
            url = getDownloadUrl(fileInfo);
            break;
        case VIEW:
            url = getSharableLink(fileInfo,"view");
            break;
        case EDIT:
            url = getSharableLink(fileInfo,"edit");
            break;
        case EMBED:
            url = getEmbedUrl(fileInfo);
            break;
        }
        return url == null ? null : asURI(url);
    }

    @Override
    public InputStream getStream(ManagedBlob blob) {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        GraphServiceClient graphClient = getGraphClient(fileInfo);
        Drive drive = graphClient.me().drive().get();
        return getDriveItemRequestBuilder(fileInfo).content().get();

    }

    @Override
    public InputStream getThumbnail(ManagedBlob blob) {
        LiveConnectFileInfo fileInfo = toFileInfo(blob);
        List<ThumbnailSet> thumbnailSets = getDriveItemRequestBuilder(fileInfo).thumbnails().get().getValue();

        if (thumbnailSets.isEmpty()) {
            return null;
        } else {
            byte[] content;
            ThumbnailSet thumbnailSet = thumbnailSets.get(0);

            if (thumbnailSet.getLarge() != null) {
               content = thumbnailSet.getLarge().getContent();
            } else if (thumbnailSet.getMedium() != null) {
                content = thumbnailSet.getMedium().getContent();
            } else if (thumbnailSet.getSmall() != null) {
                content = thumbnailSet.getSmall().getContent();
            } else {
                return null;
            }
            return content != null ? new ByteArrayInputStream(content) : null;
        }
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
        String appUrl = getSharableLink(fileInfo,"edit");
        appLink.setLink(appUrl);
        appLink.setAppName("Microsoft OneDrive");
        appLink.setIcon(baseUrl + "/icons/OneDrive.png");
        return Collections.singletonList(appLink);
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return new MSGraphDriveItemLiveConnectFile(fileInfo, getDriveItem(fileInfo));
    }


    protected GraphServiceClient getGraphClient(LiveConnectFileInfo fileInfo) {
        try {
            return getGraphClient(getCredential(fileInfo));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected GraphServiceClient getGraphClient(Credential credential) {
        return getOAuth2Provider().getGraphClient(credential);
    }

    protected DriveItemItemRequestBuilder getDriveItemRequestBuilder(LiveConnectFileInfo fileInfo) {
        GraphServiceClient graphClient = getGraphClient(fileInfo);
        Drive drive = graphClient.me().drive().get();
        return graphClient.drives().byDriveId(drive.getId()).items().byDriveItemId(fileInfo.getFileId());
    }

    protected DriveItem getDriveItem(LiveConnectFileInfo fileInfo) throws IOException {
        return getDriveItemRequestBuilder(fileInfo).get();
    }

    protected String getSharableLink(LiveConnectFileInfo fileInfo, String type) throws IOException {
        CreateLinkPostRequestBody createLinkPostRequestBody = new CreateLinkPostRequestBody();
        createLinkPostRequestBody.setType(type);
        createLinkPostRequestBody.setScope("anonymous");
        createLinkPostRequestBody.setRetainInheritedPermissions(false);
        Permission permission = getDriveItemRequestBuilder(fileInfo).createLink().post(createLinkPostRequestBody);
        return permission.getLink().getWebUrl();
    }

    protected String getEmbedUrl(LiveConnectFileInfo fileInfo) throws IOException {
        GraphServiceClient graphClient = getGraphClient(fileInfo);
        Drive drive = graphClient.me().drive().get();
        if ("business".equals(drive.getDriveType())) {
            PreviewPostRequestBody previewPostRequestBody = new PreviewPostRequestBody();
            ItemPreviewInfo preview = getDriveItemRequestBuilder(fileInfo).preview().post(previewPostRequestBody);
            return preview.getGetUrl();
        } else {
            return getSharableLink(fileInfo,"embed");
        }
    }

    protected String getDownloadUrl(LiveConnectFileInfo fileInfo) throws IOException {
        DriveItem item = getDriveItem(fileInfo);
        return (String) item.getAdditionalData().get("@microsoft.graph.downloadUrl");
    }

}
