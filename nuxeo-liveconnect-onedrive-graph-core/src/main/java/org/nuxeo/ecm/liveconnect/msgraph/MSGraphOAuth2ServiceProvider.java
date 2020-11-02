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
 *     Michael Vachette
 */
package org.nuxeo.ecm.liveconnect.msgraph;

import com.google.api.client.auth.oauth2.Credential;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;

import java.io.IOException;

public class MSGraphOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    private static final Log log = LogFactory.getLog(MSGraphOAuth2ServiceProvider.class);

    protected IGraphServiceClient getGraphClient(Credential credential) {
        String accessToken = credential.getAccessToken();
        return getGraphClient(accessToken);
    }

    protected IGraphServiceClient getGraphClient(String accessToken) {
        return GraphServiceClient.builder()
                .authenticationProvider(request -> {
                    request.addHeader("Authorization", "Bearer " + accessToken);
                })
                .buildClient();
    }

    @Override
    protected String getUserEmail(String accessToken) throws IOException {
        IGraphServiceClient graphClient = getGraphClient(accessToken);
        User user =  graphClient.me().buildRequest().get();
        return user.userPrincipalName;
    }

}
