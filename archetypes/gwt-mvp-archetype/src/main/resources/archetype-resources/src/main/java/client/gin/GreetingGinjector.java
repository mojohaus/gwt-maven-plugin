#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.client.gin;

import net.customware.gwt.dispatch.client.gin.ClientDispatchModule;
import net.customware.gwt.presenter.client.place.PlaceManager;
import ${package}.client.mvp.AppPresenter;
import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

@GinModules({ ClientDispatchModule.class, GreetingClientModule.class })
public interface GreetingGinjector extends Ginjector {

	AppPresenter getAppPresenter();

	PlaceManager getPlaceManager();

}
