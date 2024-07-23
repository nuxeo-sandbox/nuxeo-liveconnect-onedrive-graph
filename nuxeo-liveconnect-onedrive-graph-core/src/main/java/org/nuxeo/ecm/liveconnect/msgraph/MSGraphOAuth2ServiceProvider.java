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

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.google.api.client.auth.oauth2.Credential;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;
import reactor.core.publisher.Mono;

public class MSGraphOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    private static final Logger log = LogManager.getLogger(MSGraphOAuth2ServiceProvider.class);

    protected GraphServiceClient getGraphClient(Credential credential) {
        String accessToken = credential.getAccessToken();
        return getGraphClient(accessToken);
    }

    protected GraphServiceClient getGraphClient(String accessToken) {
        TokenCredential tokenCredential = tokenRequestContext -> Mono.just(new AccessToken(accessToken,null));
        return new GraphServiceClient(tokenCredential, this.getScopes().toArray(String[]::new));
    }

    @Override
    protected String getUserEmail(String accessToken) {
        GraphServiceClient graphClient = getGraphClient(accessToken);
        User user =  graphClient.me().get();
        return user.getMail();
    }

}
