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
import com.google.api.client.auth.oauth2.Credential;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;
import reactor.core.publisher.Mono;

public class MSGraphOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    private static final Log log = LogFactory.getLog(MSGraphOAuth2ServiceProvider.class);

    protected GraphServiceClient<Request> getGraphClient(Credential credential) {
        String accessToken = credential.getAccessToken();
        return getGraphClient(accessToken);
    }

    protected GraphServiceClient<Request> getGraphClient(String accessToken) {

        return GraphServiceClient.builder()
                .authenticationProvider(new TokenCredentialAuthProvider(this.getScopes(),
                        tokenRequestContext -> Mono.just(new AccessToken(accessToken,null))))
                .buildClient();
    }

    @Override
    protected String getUserEmail(String accessToken) {
        GraphServiceClient<Request> graphClient = getGraphClient(accessToken);
        User user =  graphClient.me().buildRequest().get();
        return user.userPrincipalName;
    }

}
