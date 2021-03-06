/*
 *   This software is distributed under the terms of the FSF 
 *   Gnu Lesser General Public License (see lgpl.txt). 
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.web.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.scooterframework.common.util.CurrentThreadCache;
import com.scooterframework.common.util.WordUtil;
import com.scooterframework.orm.activerecord.ActiveRecord;
import com.scooterframework.orm.activerecord.ActiveRecordUtil;
import com.scooterframework.orm.sqldataexpress.object.RESTified;

/**
 * <p>F (FormHelper) class has helper methods for http form. </p>
 * 
 * <p>In order to display validation messages for a form element, 
 * <tt>formForOpen</tt>, <tt>formForClose</tt> and <tt>label</tt> methods 
 * must be used together. If there is error for a field, the field's label 
 * element will be highlighted. </p>
 * 
 * @author (Fei) John Chen
 * 
 */
public class F {
    private static final String CURRENT_FORM_OBJECT_STACK     = "scooter.current.form.object.stack";
    private static final String CURRENT_FORM_OBJECT_KEY_STACK = "scooter.current.form.object.key.stack";
    
    private static RESTified getAndValidateObject(String resource, Object objectKey) {
        if (objectKey == null) return null;
        
        Object object = W.get(objectKey.toString());
        if (object != null && !(object instanceof RESTified)) {
            throw new IllegalArgumentException("The object which maps to key \"" + 
            objectKey + "\" for resource \"" + resource + 
            "\" must be of RESTified type, but instead it is of \"" + 
            object.getClass().getName() + "\" type.");
        }
        return (RESTified)object;
    }
    
    private static RESTified validateObject(String resource, Object object) {
        if (object == null) return null;
        
        if (object != null && !(object instanceof RESTified)) {
            throw new IllegalArgumentException("The object \"" + 
            object + "\" for resource \"" + resource + 
            "\" must be of RESTified type, but instead it is of \"" + 
            object.getClass().getName() + "\" type.");
        }
        return (RESTified)object;
    }
    
    private static RESTified validateParentObject(String resource, Object object) {
        if (object == null) return null;
        
        if (object != null && 
            !(object instanceof RESTified) && !(object instanceof String)) {
            throw new IllegalArgumentException("The object \"" + 
            object + "\" for resource \"" + resource + 
            "\" must be of either String or RESTified type, but instead it is of \"" + 
            object.getClass().getName() + "\" type.");
        }
        return (RESTified)object;
    }
    
    private static Object[] validateParentObject(String[] parentResources, Object[] parentRestfuls) {
        if ((parentResources == null) || (parentRestfuls == null)) return null;
        if (parentResources.length != parentRestfuls.length) 
            throw new IllegalArgumentException("The length of parent resources " + 
            "must be equal to the length of the parent objects.");
        
        int length = parentResources.length;
        for (int i = 0; i < length; i++) {
            parentRestfuls[i] = validateParentObject(parentResources[i], parentRestfuls[i]);
        }
        return parentRestfuls;
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a resource. 
     * The <tt>objectKey</tt> is used to retrieve the corresponding object for 
     * the resource. The object must be of RESTified type. If there is no 
     * object mapped to the key, or the object's restful id is null, the form 
     * is for adding a new object. Otherwise it is for editing the object.</p>
     * 
     * @param resourceName      name of the resource
     * @param objectKey         key pointing to the object
     * @return form-open element for a resource object
     */
    public static String formForOpen(String resourceName, String objectKey) {
        RESTified object = getAndValidateObject(resourceName, objectKey);
        storeObjectToCurrentCache(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForResource(resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a resource. 
     * If the object's restful id is null, the form is for adding a new object. 
     * Otherwise it is for editing the object.</p>
     * 
     * @param resourceName      name of the resource
     * @param object            an ActiveRecord object
     * @return form-open element for a resource object
     */
    public static String formForOpen(String resourceName, ActiveRecord object) {
        RESTified record = getAndValidateObject(resourceName, object);
        storeObjectToCurrentCache(object);
        String objectKey = ActiveRecordUtil.getModelName(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForResource(resourceName, record);
    }
    
    /**
     * Returns form-end element for a resource. 
     * 
     * @param resourceName      name of the resource
     * @return form-end element for a resource object
     */
    public static String formForClose(String resourceName) {
        removeObjectFromCurrentCache();
        removeObjectKeyFromCurrentCache();
        return "</form>";
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource. </p>
     * 
     * <p>The <tt>parentObjectKey</tt> is used to retrieve the corresponding 
     * object for the parent resource. The parent object must be of RESTified 
     * type. </p>
     * 
     * <p>The <tt>objectKey</tt> is used to retrieve the corresponding object for 
     * the nested resource. The object must be of RESTified type. </p>
     * 
     * <p>If there is no object mapped to the key, or the object's restful id is 
     * null, the form is for adding a new object. Otherwise it is for editing 
     * the object.</p>
     * 
     * @param parentResourceName    name of the parent resource
     * @param parentObjectKey       key pointing to the parent object
     * @param resourceName          name of the nested resource
     * @param objectKey             key pointing to the object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String parentResourceName, 
            String parentObjectKey, String resourceName, String objectKey) {
        RESTified parentRecord = getAndValidateObject(parentResourceName, parentObjectKey);
        RESTified object = getAndValidateObject(resourceName, objectKey);
        storeObjectToCurrentCache(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceName, parentRecord, resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource.</p> 
     * 
     * <p>The <tt>parentObjectKey</tt> is used to retrieve the corresponding 
     * object for the parent resource. The parent object must be of RESTified type. </p>
     * 
     * <p>If ActiveRecord object's restful id is null, the form is for adding a 
     * new object. Otherwise it is for editing the object.</p>
     * 
     * @param parentResourceName    name of the parent resource
     * @param parentObjectKey       key pointing to the parent object
     * @param resourceName          name of the nested resource
     * @param object                an ActiveRecord object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String parentResourceName, 
            String parentObjectKey, String resourceName, ActiveRecord object) {
        RESTified parentRecord = getAndValidateObject(parentResourceName, parentObjectKey);
        storeObjectToCurrentCache(object);
        String objectKey = ActiveRecordUtil.getModelName(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceName, parentRecord, resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource. </p>
     * 
     * <p>The <tt>objectKey</tt> is used to retrieve the corresponding object for 
     * the nested resource. The object must be of RESTified type. </p>
     * 
     * <p>If there is no object mapped to the key, or the object's restful id is 
     * null, the form is for adding a new object. Otherwise it is for editing 
     * the object.</p>
     * 
     * @param parentResourceName    name of the parent resource
     * @param parentObject          the parent object
     * @param resourceName          name of the nested resource
     * @param objectKey             key pointing to the object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String parentResourceName, 
            RESTified parentObject, String resourceName, String objectKey) {
        RESTified parentRecord = validateObject(parentResourceName, parentObject);
        RESTified object = getAndValidateObject(resourceName, objectKey);
        storeObjectToCurrentCache(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceName, parentRecord, resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource. </p>
     * 
     * <p>If ActiveRecord object's restful id is null, the form is for adding a 
     * new object. Otherwise it is for editing the object.</p>
     * 
     * @param parentResourceName    name of the parent resource
     * @param parentObject          the parent object
     * @param resourceName          name of the nested resource
     * @param object                an ActiveRecord object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String parentResourceName, 
            RESTified parentObject, String resourceName, ActiveRecord object) {
        RESTified parentRecord = validateObject(parentResourceName, parentObject);
        storeObjectToCurrentCache(object);
        String objectKey = ActiveRecordUtil.getModelName(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceName, parentRecord, resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource.</p>
     * 
     * <p><tt>parentResourceNames</tt> is an array of ancestors. The oldest is 
     * at the beginning of the array. 
     * <tt>parentRestfuls</tt> is an array of either restful id strings or 
     * RESTified records of ancestors. </p>
     * 
     * <p>The <tt>objectKey</tt> is used to retrieve the corresponding object for 
     * the nested resource. The object must be of RESTified type. </p>
     * 
     * <p>If there is no object mapped to the key, or the object's restful id is 
     * null, the form is for adding a new object. Otherwise it is for editing 
     * the object.</p>
     * 
     * @param parentResourceNames   names of the parent resources
     * @param parentRestfuls        the parent restful ids or RESTified objects
     * @param resourceName          name of the nested resource
     * @param objectKey             key pointing to the object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String[] parentResourceNames, 
            Object[] parentRestfuls, String resourceName, String objectKey) {
        Object[] parentRecords = validateParentObject(parentResourceNames, parentRestfuls);
        RESTified object = getAndValidateObject(resourceName, objectKey);
        storeObjectToCurrentCache(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceNames, parentRecords, resourceName, object);
    }
    
    /**
     * <p>Returns form-open element <tt>&lt;form&gt;</tt>for a nested resource.</p>
     * 
     * <p><tt>parentResourceNames</tt> is an array of ancestors. The oldest is 
     * at the beginning of the array. 
     * <tt>parentRestfuls</tt> is an array of either restful id strings or 
     * RESTified records of ancestors. </p>
     * 
     * <p>If ActiveRecord object's restful id is null, the form is for adding a 
     * new object. Otherwise it is for editing the object.</p>
     * 
     * @param parentResourceNames   names of the parent resources
     * @param parentRestfuls        the parent restful ids or RESTified objects
     * @param resourceName          name of the nested resource
     * @param object                an ActiveRecord object
     * @return form-open element for a nested resource object
     */
    public static String formForOpen(String[] parentResourceNames, 
            Object[] parentRestfuls, String resourceName, ActiveRecord object) {
        Object[] parentRecords = validateParentObject(parentResourceNames, parentRestfuls);
        storeObjectToCurrentCache(object);
        String objectKey = ActiveRecordUtil.getModelName(object);
        storeObjectKeyToCurrentCache(objectKey);
        return R.formForNestedResourceRecord(parentResourceNames, parentRecords, resourceName, object);
    }
    
    /**
     * Returns label string for a field of the underline object.
     * 
     * This method should not be used without using <tt>formFor</tt> 
     * method first.
     * 
     * <pre>
     *  Examples:
     *      <label for="post_name" >Name</label>
     *      <div class="fieldWithErrors"><label for="post_title" >Title</label></div><br />
     * </pre>
     * @param field     field name
     * @return error-aware label tag string
     */
    public static String label(String field) {
        return label(field, null);
    }
    
    /**
     * Returns label string for a field of the underline object.
     * 
     * This method should not be used without using <tt>formFor</tt> 
     * method first.
     * 
     * <pre>
     *  Examples:
     *      <label for="post_name" >Name</label>
     *      <div class="fieldWithErrors"><label for="post_title" >Title</label></div><br />
     * </pre>
     * @param field     field name
     * @param options   options for the label tag
     * @return error-aware label tag string
     */
    public static String label(String field, Map options) {
        if (options == null) options = new HashMap();
        Object object = getObjectFromCurrentCache();
        Object objectKey = getObjectKeyFromCurrentCache();
        options.put("for", tagId(objectKey, field));
        return W.taggedContent(object, field, "label", WordUtil.titleize(field), options);
    }
    
    private static String tagId(Object objectKey, String field) {
        return objectKey + "_" + field;
    }
    
    private static Stack getCurrentCacheObjectStack() {
        Stack stk = (Stack)CurrentThreadCache.get(CURRENT_FORM_OBJECT_STACK);
        if (stk == null) {
            stk = new Stack();
            CurrentThreadCache.set(CURRENT_FORM_OBJECT_STACK, stk);
        }
        return stk;
    }
    
    private static Object getObjectFromCurrentCache() {
        return getCurrentCacheObjectStack().peek();
    }
    
    private static void storeObjectToCurrentCache(Object object) {
        getCurrentCacheObjectStack().push(object);
    }
    
    private static void removeObjectFromCurrentCache() {
        Stack stk = getCurrentCacheObjectStack();
        stk.pop();
        if (stk.empty()) {
            CurrentThreadCache.clear(CURRENT_FORM_OBJECT_STACK);
        }
    }
    
    private static Stack getCurrentCacheObjectKeyStack() {
        Stack stk = (Stack)CurrentThreadCache.get(CURRENT_FORM_OBJECT_KEY_STACK);
        if (stk == null) {
            stk = new Stack();
            CurrentThreadCache.set(CURRENT_FORM_OBJECT_KEY_STACK, stk);
        }
        return stk;
    }
    
    private static Object getObjectKeyFromCurrentCache() {
        return getCurrentCacheObjectKeyStack().peek();
    }
    
    private static void storeObjectKeyToCurrentCache(Object objectKey) {
        getCurrentCacheObjectKeyStack().push(objectKey);
    }
    
    private static void removeObjectKeyFromCurrentCache() {
        Stack stk = getCurrentCacheObjectKeyStack();
        stk.pop();
        if (stk.empty()) {
            CurrentThreadCache.clear(CURRENT_FORM_OBJECT_KEY_STACK);
        }
    }
}
