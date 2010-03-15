#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.client;

import ${package}.client.gin.GreetingGinjector;
import ${package}.client.mvp.AppPresenter;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

public class GreetMvp implements EntryPoint {
	private final GreetingGinjector injector = GWT.create(GreetingGinjector.class);

	public void onModuleLoad() {
		final AppPresenter appPresenter = injector.getAppPresenter();
		appPresenter.go(RootPanel.get());

		injector.getPlaceManager().fireCurrentPlace();
	}
}
