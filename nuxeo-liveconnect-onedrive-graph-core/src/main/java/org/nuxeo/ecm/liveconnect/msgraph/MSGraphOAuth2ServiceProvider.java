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
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.liveconnect.core.AbstractLiveConnectOAuth2ServiceProvider;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class MSGraphOAuth2ServiceProvider extends AbstractLiveConnectOAuth2ServiceProvider {

    private static final Logger log = LogManager.getLogger(MSGraphOAuth2ServiceProvider.class);

    @Override
    public Credential handleAuthorizationCallback(HttpServletRequest request) {

        // Checking if there was an error such as the user denied access
        String error = getError(request);
        if (error != null) {
            throw new NuxeoException("There was an error: \"" + error + "\".");
        }

        // Checking conditions on the "code" URL parameter
        String code = getAuthorizationCode(request);
        if (code == null) {
            throw new NuxeoException("There is not code provided as QueryParam.");
        }

        try {
            AuthorizationCodeFlow flow = getAuthorizationCodeFlow();

            String redirectUri = getCallbackUrl(request);
            TokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setScopes(getScopes().isEmpty() ? null : getScopes()) // some providers do not
                    // support the 'scopes' param
                    .setRedirectUri(redirectUri)
                    .set("client_id",getClientId())
                    .set("client_secret",getClientSecret())
                    .execute();

            // Create a unique userId to use with the credential store
            String userId = getOrCreateServiceUser(request, tokenResponse.getAccessToken());

            return flow.createAndStoreCredential(tokenResponse, userId);
        } catch (IOException e) {
            throw new NuxeoException("Failed to retrieve credential", e);
        }
    }

    @Override
    protected String getUserEmail(String accessToken) {
        GraphServiceClient graphClient = getGraphClient(accessToken);
        User user =  graphClient.me().get();
        return user.getMail();
    }

    protected GraphServiceClient getGraphClient(Credential credential) {
        String accessToken = credential.getAccessToken();
        return getGraphClient(accessToken);
    }

    protected GraphServiceClient getGraphClient(String accessToken) {
        TokenCredential tokenCredential = tokenRequestContext -> Mono.just(new AccessToken(accessToken,null));
        return new GraphServiceClient(tokenCredential, this.getScopes().toArray(String[]::new));
    }


}
