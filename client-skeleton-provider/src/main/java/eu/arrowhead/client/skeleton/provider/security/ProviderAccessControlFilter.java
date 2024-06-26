/*********************************************************************
* Copyright (c) 2024 Aparajita Tripathy 
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package eu.arrowhead.client.skeleton.provider.security;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import ai.aitia.arrowhead.application.library.util.ApplicationCommonConstants;
import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.security.AccessControlFilter;

@Component
@ConditionalOnExpression(CommonConstants.$SERVER_SSL_ENABLED_WD + " and !" + ApplicationCommonConstants.$TOKEN_SECURITY_FILTER_ENABLED_WD)
public class ProviderAccessControlFilter extends AccessControlFilter {
	
	@Override
	protected void checkClientAuthorized(final String clientCN, final String method, final String requestTarget, final String requestJSON, final Map<String,String[]> queryParams) {
		super.checkClientAuthorized(clientCN, method, requestTarget, requestJSON, queryParams);
		
		//TODO: implement here your custom access filter if any further
	}
}
