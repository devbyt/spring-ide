/*******************************************************************************
 * Copyright (c) 2005, 2008 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.beans.core.model.process;

import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfig;

/**
 * Post processing context.
 * @author Christian Dupuis
 * @since 2.0
 */
public interface IBeansConfigPostProcessingContext {

	/**
	 * Returns a {@link BeanNameGenerator}
	 */
	BeanNameGenerator getBeanNameGenerator();

	/**
	 * Returns the {@link ProblemReporter} that should be used to report errors, warnings and notes.
	 */
	ProblemReporter getProblemReporter();

	/**
	 * Returns the {@link IBeansConfigRegistrationSupport}.
	 */
	IBeansConfigRegistrationSupport getBeansConfigRegistrySupport();
	
	/**
	 * Returns the current {@link IBeansConfig} that is being post processed. 
	 * @since 2.2.1
	 */
	IBeansConfig getBeansConfig();

}
