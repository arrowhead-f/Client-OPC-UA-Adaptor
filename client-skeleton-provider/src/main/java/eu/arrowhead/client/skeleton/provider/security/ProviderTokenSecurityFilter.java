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

import java.security.PrivateKey;
import java.security.PublicKey;

import eu.arrowhead.common.token.TokenSecurityFilter;

public class ProviderTokenSecurityFilter extends TokenSecurityFilter {
	
	//=================================================================================================
	// members
	
	private PrivateKey myPrivateKey;
	private PublicKey authorizationPublicKey;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected PrivateKey getMyPrivateKey() {
		return myPrivateKey;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicKey getAuthorizationPublicKey() {
		return authorizationPublicKey;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMyPrivateKey(final PrivateKey myPrivateKey) {
		this.myPrivateKey = myPrivateKey;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAuthorizationPublicKey(final PublicKey authorizationPublicKey) {
		this.authorizationPublicKey = authorizationPublicKey;
	}	
}
