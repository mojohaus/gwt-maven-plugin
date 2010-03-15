#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.client.gin;

import net.customware.gwt.presenter.client.DefaultEventBus;
import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.gin.AbstractPresenterModule;
import net.customware.gwt.presenter.client.place.PlaceManager;
import ${package}.client.CachingDispatchAsync;
import ${package}.client.mvp.AppPresenter;
import ${package}.client.mvp.GreetingPresenter;
import ${package}.client.mvp.GreetingResponsePresenter;
import ${package}.client.mvp.GreetingResponseView;
import ${package}.client.mvp.GreetingView;

import com.google.inject.Singleton;

public class GreetingClientModule extends AbstractPresenterModule {

	@Override
	protected void configure() {		
		bind(EventBus.class).to(DefaultEventBus.class).in(Singleton.class);
		bind(PlaceManager.class).in(Singleton.class);
		
		bindPresenter(GreetingPresenter.class, GreetingPresenter.Display.class, GreetingView.class);
		bindPresenter(GreetingResponsePresenter.class, GreetingResponsePresenter.Display.class, GreetingResponseView.class);
		
		bind(AppPresenter.class).in(Singleton.class);
		bind(CachingDispatchAsync.class);
	}
}
