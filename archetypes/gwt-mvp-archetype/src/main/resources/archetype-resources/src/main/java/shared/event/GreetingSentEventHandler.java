#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.event;

import com.google.gwt.event.shared.EventHandler;

public interface GreetingSentEventHandler extends EventHandler {

	void onGreetingSent(GreetingSentEvent event);

}
