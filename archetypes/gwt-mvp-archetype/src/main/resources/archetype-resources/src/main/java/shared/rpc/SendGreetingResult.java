#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.shared.rpc;

import net.customware.gwt.dispatch.shared.Result;

public class SendGreetingResult implements Result {

	private static final long serialVersionUID = 7917449246674223581L;

	private String name;
	private String message;

	public SendGreetingResult(final String name, final String message) {
		this.name = name;
		this.message = message;
	}

	@SuppressWarnings("unused")
	private SendGreetingResult() {
	}

	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}
}
