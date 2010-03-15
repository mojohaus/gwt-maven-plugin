#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.client.mvp;

import net.customware.gwt.dispatch.client.DispatchAsync;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.inject.Inject;

public class AppPresenter {
	private HasWidgets container;
	private GreetingPresenter greetingPresenter;

	@Inject
	public AppPresenter(final DispatchAsync dispatcher,
						final GreetingPresenter greetingPresenter) {
		this.greetingPresenter = greetingPresenter;		
	}
	
	private void showMain() {
		container.clear();
		container.add(greetingPresenter.getDisplay().asWidget());
	}
		
	public void go(final HasWidgets container) {
		this.container = container;
		
		showMain();
	}
}
