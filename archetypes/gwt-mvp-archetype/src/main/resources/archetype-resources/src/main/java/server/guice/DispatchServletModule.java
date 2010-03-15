#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.server.guice;

import net.customware.gwt.dispatch.server.service.DispatchServiceServlet;
import com.google.inject.servlet.ServletModule;

public class DispatchServletModule extends ServletModule {

	@Override
	public void configureServlets() {
		// NOTE: the servlet context will probably need changing
		serve("/${artifactId}/dispatch").with(DispatchServiceServlet.class);
	}

}
