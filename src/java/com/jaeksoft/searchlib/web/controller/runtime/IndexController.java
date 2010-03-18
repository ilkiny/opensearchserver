/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2009 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web.controller.runtime;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.index.IndexAbstract;
import com.jaeksoft.searchlib.index.IndexGroup;
import com.jaeksoft.searchlib.web.controller.CommonController;

public class IndexController extends CommonController {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4160847940908204696L;

	public IndexController() throws SearchLibException {
		super();
	}

	public List<IndexAbstract> getIndices() throws SearchLibException,
			NamingException {
		List<IndexAbstract> list = new ArrayList<IndexAbstract>();
		Client client = getClient();
		if (client == null)
			return list;
		IndexAbstract index = client.getIndex();
		if (index instanceof IndexGroup) {
			for (IndexAbstract idx : ((IndexGroup) index).getIndices())
				list.add(idx);
		} else
			list.add(index);
		return list;
	}

	@Override
	public void reset() {
	}

}
