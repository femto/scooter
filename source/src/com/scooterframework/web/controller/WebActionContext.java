/*
 *   This software is distributed under the terms of the FSF 
 *   Gnu Lesser General Public License (see lgpl.txt). 
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.web.controller;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.scooterframework.common.exception.GenericException;

/**
 * <p>
 * WebActionContext class holds context data for current web request.
 * </p>
 * 
 * @author (Fei) John Chen
 */
public class WebActionContext extends ActionContext {
    
    public WebActionContext(HttpServletRequest servletRequest,
                            HttpServletResponse servletResponse) {
        super();
        initializeContext(servletRequest, servletResponse);
    }
    
    public void initializeContext(HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        resetFlashMessage();
    }
    
    /**
     * Sets the underlying Servlet Request.
     *
     */
    public void setHttpServletRequest(HttpServletRequest request) {
        this.servletRequest = request;
    }
    
    /**
     * Retrieves the underlying Servlet Request.
     *
     * @return HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest(){
        if (servletRequest == null) throw new GenericException("HttpServletRequest must be set by framework.");
        return servletRequest;
    }
    
    /**
     * Sets the underlying Servlet Response.
     *
     */
    public void setHttpServletResponse(HttpServletResponse response) {
        this.servletResponse = response;
    }
    
    /**
     * Retrieves the underlying Servlet Response.
     *
     * @return HttpServletResponse
     */
    public HttpServletResponse getHttpServletResponse() {
        return servletResponse;
    }
    
    
    /**
     * Gets data in parameter scope as a map. The value corresponding to a key
     * may either be a string or be a string array.
     * 
     * Return guarrented: An empty map will be returned if there is no data.
     * 
     * @return Map
     */
    public Map getParameterDataAsMap() {
        Map hm = new HashMap();
        
        Enumeration en = getHttpServletRequest().getParameterNames();
        while(en.hasMoreElements()) {
            String name = (String)en.nextElement();
            String[] valueAry = getHttpServletRequest().getParameterValues(name);
            if (valueAry != null) {
                if (valueAry.length == 1) {
                    hm.put(name, valueAry[0]);
                }
                else {
                    hm.put(name, valueAry);
                }
            }
        }
        
        return hm;
    }
    
    /**
     * Gets data in request scope as a map.
     * 
     * Return guarrented: An empty map will be returned if there is no data.
     * 
     * @return Map
     */
    public Map getRequestDataAsMap() {
        Map hm = new HashMap();
        
        Enumeration en = getHttpServletRequest().getAttributeNames();
        while (en.hasMoreElements()) {
            String name  = (String)en.nextElement();
            Object value = getHttpServletRequest().getAttribute(name);
            hm.put(name, value);
        }
        
        return hm;
    }
    
    /**
     * Gets data in session scope as a map.
     * 
     * Return guarrented: An empty map will be returned if there is no data.
     * 
     * @return Map
     */
    public Map getSessionDataAsMap() {
        Map hm = new HashMap();
        
        HttpSession session = getHttpServletRequest().getSession();
        
        Enumeration en = session.getAttributeNames();
        while (en.hasMoreElements()) {
            String name  = (String)en.nextElement();
            Object value = session.getAttribute(name);
            hm.put(name, value);
        }
        
        return hm;
    }
    
    /**
     * Gets data in context scope as a map.
     * 
     * Return guarrented: An empty map will be returned if there is no data.
     * 
     * @return Map
     */
    public Map getContextDataAsMap() {
        Map hm = new HashMap();
        
        ServletContext servletContext = getHttpServletRequest().getSession().getServletContext();
        Enumeration en = servletContext.getAttributeNames();
        while (en.hasMoreElements()) {
            String name  = (String)en.nextElement();
            Object value = servletContext.getAttribute(name);
            hm.put(name, value);
        }
        
        return hm;
    }
    
    /**
     * Gets data represented by the key from the parameter scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     * @return Object
     */
    public Object getFromParameterData(String key) {
        return getParameterDataAsMap().get(key);
    }
    
    /**
     * Gets data represented by the key from the request scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     * @return Object
     */
    public Object getFromRequestData(String key) {
        return getHttpServletRequest().getAttribute(key);
    }
    
    /**
     * Gets data represented by the key from the session scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     * @return Object
     */
    public Object getFromSessionData(String key) {
        return getHttpServletRequest().getSession().getAttribute(key);
    }
    
    /**
     * Gets data represented by the key from the context scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     * @return Object
     */
    public Object getFromContextData(String key) {
        return getHttpServletRequest().getSession().getServletContext().getAttribute(key);
    }
    
    /**
     * Removes data represented by the key from request scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     */
    public void removeFromRequestData(String key) {
        getHttpServletRequest().removeAttribute(key);
    }
    
    /**
     * Removes data represented by the key from session scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     */
    public void removeFromSessionData(String key) {
        getHttpServletRequest().getSession().removeAttribute(key);
    }
    
    /**
     * Removes all data represented by the key from session scope.
     */
    public void removeAllSessionData() {
        getHttpServletRequest().getSession().invalidate();
    }
    
    /**
     * Removes data represented by the key from context scope.
     * 
     * Note: The result of this method is sensitive to the case of key string.
     * 
     * @param key
     */
    public void removeFromContextData(String key) {
        getHttpServletRequest().getSession().getServletContext().removeAttribute(key);
    }
    
    /**
     * Stores the object represented by the key to request scope.
     * 
     * @param key String
     * @param object Object
     */
    public void storeToRequest(String key, Object object) {
        getHttpServletRequest().setAttribute(key, object);
    }
    
    /**
     * Stores the object represented by the key to session scope.
     * 
     * @param key String
     * @param object Object
     */
    public void storeToSession(String key, Object object) {
        getHttpServletRequest().getSession().setAttribute(key, object);
    }
    
    /**
     * Stores the object represented by the key to context scope.
     * 
     * @param key String
     * @param object Object
     */
    public void storeToContext(String key, Object object) {
        getHttpServletRequest().getSession().getServletContext().setAttribute(key, object);
    }
    
    /**
     * Returns a named cycle from cycle map.
     * 
     * @param name
     * @return cycle object
     */
    protected Object getCycleFromCycleMap(String name) {
        return namedCycles.get(name);
    }
    
    /**
     * Sets a named cycle in cycle map.
     * 
     * @param name
     * @param cycle
     */
    protected void setCycleToCycleMap(String name, Object cycle) {
        namedCycles.put(name, cycle);
    }
    
    private Map namedCycles = new HashMap();
    
    protected HttpServletRequest servletRequest;
    protected HttpServletResponse servletResponse;
}
