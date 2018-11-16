/*
 * generated by Xtext 2.12.0
 */
package fr.zelus.jarvis.language.execution.ide

import com.google.inject.Guice
import org.eclipse.xtext.util.Modules2
import fr.zelus.jarvis.language.execution.ExecutionStandaloneSetup
import fr.zelus.jarvis.language.execution.ExecutionRuntimeModule

/**
 * Initialization support for running Xtext languages as language servers.
 */
class ExecutionIdeSetup extends ExecutionStandaloneSetup {

	override createInjector() {
		Guice.createInjector(Modules2.mixin(new ExecutionRuntimeModule, new ExecutionIdeModule))
	}
	
}