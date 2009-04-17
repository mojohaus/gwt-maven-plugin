package org.mycompany;

import java.util.List;
import com.google.gwt.user.client.rpc.RemoteService;

public interface NumberService extends RemoteService{

	public List<Number> getNumberList();

	public int getPrimitive();
	
}
