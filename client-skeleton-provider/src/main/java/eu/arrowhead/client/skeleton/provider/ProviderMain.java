/*********************************************************************
* Copyright (c) 2024 Aparajita Tripathy 
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package eu.arrowhead.client.skeleton.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import eu.arrowhead.common.CommonConstants;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@ComponentScan(basePackages = {"ai.aitia"}) //TODO: add custom packages if any
public class ProviderMain {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		SpringApplication.run(ProviderMain.class, args);

	}
}

