package org.jboss.tools.birt.core.internal.project.facet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.birt.integration.wtp.ui.internal.util.DataUtil;
import org.eclipse.birt.integration.wtp.ui.internal.util.WebArtifactUtil;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.ContextParamBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.FilterBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.FilterMappingBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.ListenerBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.ServletBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.ServletMappingBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.TagLibBean;
import org.eclipse.birt.integration.wtp.ui.internal.webapplication.WebAppBean;
import org.eclipse.birt.integration.wtp.ui.internal.wizards.SimpleImportOverwriteQuery;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.common.CommonFactory;
import org.eclipse.jst.j2ee.common.Description;
import org.eclipse.jst.j2ee.common.Listener;
import org.eclipse.jst.j2ee.common.ParamValue;
import org.eclipse.jst.j2ee.jsp.JSPConfig;
import org.eclipse.jst.j2ee.jsp.JspFactory;
import org.eclipse.jst.j2ee.jsp.TagLibRefType;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.webapplication.ContextParam;
import org.eclipse.jst.j2ee.webapplication.Filter;
import org.eclipse.jst.j2ee.webapplication.FilterMapping;
import org.eclipse.jst.j2ee.webapplication.Servlet;
import org.eclipse.jst.j2ee.webapplication.ServletType;
import org.eclipse.jst.j2ee.webapplication.TagLibRef;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipse.jst.j2ee.webapplication.WebapplicationFactory;
import org.eclipse.ui.dialogs.IOverwriteQuery;

public class JBossBirtUtil implements IBirtUtil {

	public void configureContextParam(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;
		// handle context-param settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String name = DataUtil.getString(it.next(), false);
			ContextParamBean bean = (ContextParamBean) map.get(name);
			if (bean == null)
				continue;

			// if contained this param
			List list = null;
			if (webApp.getVersionID() == 23) {
				// for servlet 2.3
				list = webApp.getContexts();
			} else {
				// for servlet 2.4
				list = webApp.getContextParams();
			}

			int index = getContextParamIndexByName(list, name);
			if (index >= 0) {
				String ret = query
						.queryOverwrite("Context-param '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$

				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}

				// remove old item
				list.remove(index);
			}

			String value = bean.getValue();
			String description = bean.getDescription();

			if (webApp.getVersionID() == 23) {
				// create context-param object
				ContextParam param = WebapplicationFactory.eINSTANCE
						.createContextParam();
				param.setParamName(name);
				param.setParamValue(value);
				if (description != null)
					param.setDescription(description);

				param.setWebApp(webApp);
			} else {
				// create ParamValue object for servlet 2.4
				ParamValue param = CommonFactory.eINSTANCE.createParamValue();
				param.setName(name);
				param.setValue(value);
				if (description != null) {
					Description descriptionObj = CommonFactory.eINSTANCE
							.createDescription();
					descriptionObj.setValue(description);
					param.getDescriptions().add(descriptionObj);
					param.setDescription(description);
				}

				// add into list
				webApp.getContextParams().add(param);
			}
		}

	}

	private int getContextParamIndexByName(List list, String name) {
		if (list == null || name == null)
			return -1;
		Iterator it = list.iterator();
		int index = 0;
		while (it.hasNext()) {
			// get param object
			Object paramObj = it.next();
			// for servlet 2.3
			if (paramObj instanceof ContextParam) {
				ContextParam param = (ContextParam) paramObj;
				if (name.equals(param.getParamName()))
					return index;
			}
			// for servlet 2.4
			if (paramObj instanceof ParamValue) {
				ParamValue param = (ParamValue) paramObj;
				if (name.equals(param.getName()))
					return index;
			}
			index++;
		}
		return -1;
	}

	public void configureFilter(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;

		// handle filter settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String name = DataUtil.getString(it.next(), false);
			FilterBean bean = (FilterBean) map.get(name);

			if (bean == null)
				continue;

			// if contained this filter
			Object obj = webApp.getFilterNamed(name);
			if (obj != null) {
				String ret = query.queryOverwrite("Filter '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$

				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}

				// remove old item
				webApp.getFilters().remove(obj);
			}

			String className = bean.getClassName();
			String description = bean.getDescription();

			// create filter object
			Filter filter = WebapplicationFactory.eINSTANCE.createFilter();
			filter.setName(name);
			filter.setFilterClassName(className);
			filter.setDescription(description);
			webApp.getFilters().add(filter);
		}
	}

	public void configureFilterMapping(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;

		// handle filter-mapping settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = DataUtil.getString(it.next(), false);
			FilterMappingBean bean = (FilterMappingBean) map.get(key);
			if (bean == null)
				continue;
			// if contained this filter-mapping
			Object obj = getFilterMappingByKey(webApp.getFilterMappings(), key);
			if (obj != null) {
				String ret = query
						.queryOverwrite("Filter-mapping '" + key + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}
				// remove old item
				webApp.getFilterMappings().remove(obj);
			}
			// filter name
			String name = bean.getName();
			// create FilterMapping object
			FilterMapping mapping = WebapplicationFactory.eINSTANCE
					.createFilterMapping();
			// get filter by name
			Filter filter = webApp.getFilterNamed(name);
			if (filter != null) {
				mapping.setFilter(filter);
				mapping.setUrlPattern(bean.getUri());
				mapping.setServletName(bean.getServletName());
				// get Servlet object
				Servlet servlet = webApp.getServletNamed(bean.getServletName());
				mapping.setServlet(servlet);
				if (bean.getUri() != null || servlet != null)
					webApp.getFilterMappings().add(mapping);
			}
		}
	}

	private Object getFilterMappingByKey(List list, String key) {
		if (list == null || key == null)
			return null;
		Iterator it = list.iterator();
		while (it.hasNext()) {
			// get filter-mapping object
			FilterMapping filterMapping = (FilterMapping) it.next();
			if (filterMapping != null) {
				String name = filterMapping.getFilter().getName();
				String servletName = filterMapping.getServletName();
				String uri = filterMapping.getUrlPattern();
				String curKey = getFilterMappingString(name, servletName, uri);
				if (key.equals(curKey))
					return filterMapping;
			}
		}
		return null;
	}

	private String getFilterMappingString(String name, String servletName,
			String uri) {
		return (name != null ? name : "") //$NON-NLS-1$
				+ (servletName != null ? servletName : "") //$NON-NLS-1$
				+ (uri != null ? uri : ""); //$NON-NLS-1$
	}

	public void configureListener(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;
		// handle listeners settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String name = DataUtil.getString(it.next(), false);
			ListenerBean bean = (ListenerBean) map.get(name);
			if (bean == null)
				continue;

			String className = bean.getClassName();
			String description = bean.getDescription();

			// if listener existed in web.xml, skip it
			Object obj = getListenerByClassName(webApp.getListeners(),
					className);
			if (obj != null)
				continue;

			// create Listener object
			Listener listener = CommonFactory.eINSTANCE.createListener();
			listener.setListenerClassName(className);
			if (description != null)
				listener.setDescription(description);

			webApp.getListeners().remove(listener);
			webApp.getListeners().add(listener);
		}
	}

	private Object getListenerByClassName(List list, String className) {
		if (list == null || className == null)
			return null;
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Listener listener = (Listener) it.next();
			if (listener != null
					&& className.equals(listener.getListenerClassName())) {
				return listener;
			}
		}
		return null;
	}

	public void configureServlet(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;
		// handle servlet settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String name = DataUtil.getString(it.next(), false);
			ServletBean bean = (ServletBean) map.get(name);
			if (bean == null)
				continue;
			// if contained this servlet
			Object obj = webApp.getServletNamed(name);
			if (obj != null) {
				String ret = query.queryOverwrite("Servlet '" + name + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}
				// remove old item
				webApp.getServlets().remove(obj);
			}
			String className = bean.getClassName();
			String description = bean.getDescription();
			// create Servlet Type object
			ServletType servletType = WebapplicationFactory.eINSTANCE
					.createServletType();
			servletType.setClassName(className);
			// create Servlet object
			Servlet servlet = WebapplicationFactory.eINSTANCE.createServlet();
			servlet.setServletName(name);
			if (description != null)
				servlet.setDescription(description);
			servlet.setWebType(servletType);
			servlet.setWebApp(webApp);
		}

	}

	private WebApp getWebApp(Map map, IProject project, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return null;
		if (map == null || project == null) {
			return null;
		}
		IModelProvider modelProvider = ModelProviderManager
				.getModelProvider(project);
		Object modelObject = modelProvider.getModelObject();
		if (!(modelObject instanceof WebApp)) {
			// TODO log
			return null;
		}
		return (WebApp) modelObject;
	}

	public void configureServletMapping(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String uri = DataUtil.getString(it.next(), false);
			ServletMappingBean bean = (ServletMappingBean) map.get(uri);

			if (bean == null)
				continue;

			// if contained this servlet-mapping
			Object obj = WebArtifactUtil.getServletMappingByUri(webApp
					.getServletMappings(), uri);
			if (obj != null) {
				String ret = query
						.queryOverwrite("Servlet-mapping '" + uri + "'"); //$NON-NLS-1$ //$NON-NLS-2$

				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}

				// remove old item
				webApp.getServletMappings().remove(obj);
			}

			// servlet name
			String name = bean.getName();

			// create ServletMapping object
			org.eclipse.jst.j2ee.webapplication.ServletMapping mapping = WebapplicationFactory.eINSTANCE
					.createServletMapping();

			// get servlet by name
			Servlet servlet = webApp.getServletNamed(name);
			if (servlet != null) {
				mapping.setServlet(servlet);
				mapping.setUrlPattern(uri);
				mapping.setWebApp(webApp);
			}
		}
	}

	public void configureTaglib(Map map, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		WebApp webApp = getWebApp(map, project, monitor);
		if (webApp == null)
			return;
		// handle taglib settings
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String uri = DataUtil.getString(it.next(), false);
			TagLibBean bean = (TagLibBean) map.get(uri);

			if (bean == null)
				continue;

			// if contained this taglib
			Object obj = getTagLibByUri(webApp, uri);
			if (obj != null) {
				String ret = query.queryOverwrite("Taglib '" + uri + "'"); //$NON-NLS-1$ //$NON-NLS-2$

				// check overwrite query result
				if (IOverwriteQuery.NO.equalsIgnoreCase(ret)) {
					continue;
				}
				if (IOverwriteQuery.CANCEL.equalsIgnoreCase(ret)) {
					monitor.setCanceled(true);
					return;
				}

				// remove old item
				if (obj instanceof TagLibRefType
						&& webApp.getJspConfig() != null) {
					webApp.getJspConfig().getTagLibs().remove(obj);
				} else {
					webApp.getTagLibs().remove(obj);
				}

			}

			String location = bean.getLocation();

			if (webApp.getVersionID() == 23) {
				// create TaglibRef object for servlet 2.3
				TagLibRef taglib = WebapplicationFactory.eINSTANCE
						.createTagLibRef();
				taglib.setTaglibURI(uri);
				taglib.setTaglibLocation(location);
				webApp.getTagLibs().add(taglib);
			} else {
				// for servlet 2.4
				JSPConfig jspConfig = JspFactory.eINSTANCE.createJSPConfig();
				TagLibRefType ref = JspFactory.eINSTANCE.createTagLibRefType();
				ref.setTaglibURI(uri);
				ref.setTaglibLocation(location);
				jspConfig.getTagLibs().add(ref);
				webApp.setJspConfig(jspConfig);
			}
		}

	}

	private Object getTagLibByUri(WebApp webapp, String uri) {
		if (webapp == null || uri == null)
			return null;
		List list = null;
		JSPConfig config = webapp.getJspConfig();
		if (config != null) {
			// for servlet 2.4
			list = config.getTagLibs();
		} else {
			list = webapp.getTagLibs();
		}
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Object obj = it.next();
			// for servlet 2.3
			if (obj instanceof TagLibRef) {
				TagLibRef ref = (TagLibRef) obj;
				if (uri.equals(ref.getTaglibURI()))
					return ref;
			}

			// for servlet 2.4
			if (obj instanceof TagLibRefType) {
				TagLibRefType ref = (TagLibRefType) obj;
				if (uri.equals(ref.getTaglibURI()))
					return ref;
			}
		}
		return null;
	}

	public void configureWebApp(WebAppBean webAppBean, IProject project,
			SimpleImportOverwriteQuery query, IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return;
		if (webAppBean == null || project == null) {
			return;
		}
		IModelProvider modelProvider = ModelProviderManager
				.getModelProvider(project);
		Object modelObject = modelProvider.getModelObject();
		if (!(modelObject instanceof WebApp)) {
			// TODO log
			return;
		}
		WebApp webApp = (WebApp) modelObject;
		webApp.setDescription(webAppBean.getDescription());
	}

}