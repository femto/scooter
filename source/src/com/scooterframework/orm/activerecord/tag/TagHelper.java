/*
 *   This software is distributed under the terms of the FSF 
 *   Gnu Lesser General Public License (see lgpl.txt). 
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.orm.activerecord.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.scooterframework.common.util.Converters;
import com.scooterframework.orm.activerecord.ActiveRecord;
import com.scooterframework.orm.activerecord.ActiveRecordUtil;
import com.scooterframework.orm.activerecord.AssociatedRecords;
import com.scooterframework.orm.activerecord.AssociationHelper;

/**
 * TagHelper has helper methods related to tags. 
 * 
 * @author (Fei) John Chen
 */
public class TagHelper {
    
    /**
     * Returns all tag names.
     * 
     * @return a set of tag names
     */
    public static Set allTags() {
        List tags = ActiveRecordUtil.getHomeInstance(Tag.class).findAll();
        if (tags == null) return null;
        
        Set tagSet = new HashSet();
        Iterator it = tags.iterator();
        while(it.hasNext()) {
            ActiveRecord tag = (ActiveRecord)it.next();
            tagSet.add(tag.getField("name"));
        }
        return tagSet;
    }
    
    /**
     * Adds tags to tags table. If a tag is already in the tags table, it is 
     * ignored.
     * 
     * @param tags the tags string
     */
    public static void addTags(String tags) {
        if (tags == null || "".equals(tags)) return;
        tags = tags.toLowerCase();
        
        List tagList = Converters.convertStringToList(tags);
        if (tagList == null) return;
        
        //get all current tags of the record
        Set currentTags = allTags();
        
        Iterator it = tagList.iterator();
        while(it.hasNext()) {
            String tagName = (String)it.next();
            if (currentTags != null && currentTags.contains(tagName)) continue;
            currentTags.add(tagName);
            
            ActiveRecord atag = new Tag();
            atag.setData("name=" + tagName);
            atag.create();
        }
    }

    /**
     * Registers record types that can be treated as taggable. This methods
     * must be called first before using other methods related to a target 
     * in TagHelper.
     * 
     * @param targets specific types that act as taggable.
     */
    public static void registerTaggables(Class[] targets) {
        AssociationHelper.hasManyInCategoryThrough(Tag.class, targets, "taggable", Tagging.class);
    }
    
    /**
     * Returns a set of tag names associated with a record instance.
     * 
     * @param record
     * @return a set of tag names
     */
    public static Set allTags(ActiveRecord record) {
        List tags = record.allAssociated(Tag.class).getRecords();
        if (tags == null) return null;
        
        Set tagSet = new HashSet();
        Iterator it = tags.iterator();
        while(it.hasNext()) {
            ActiveRecord tag = (ActiveRecord)it.next();
            tagSet.add(tag.getField("name"));
        }
        return tagSet;
    }
    
    /**
     * Returns a count of all tag names associated with a record instance.
     * 
     * @param record
     * @return number of tags associated with the record
     */
    public static int tagsCount(ActiveRecord record) {
        List tags = record.allAssociated(Tag.class).getRecords();
        return (tags != null)?tags.size():0;
    }
    
    /**
     * Adds tags to a record. Multiple tags can be listed in the same string 
     * separated by a delimiter. If a tag is not in the tag table, it will be 
     * added to the Tag table first. 
     * 
     * The delimiter string to separate tag names can be one of the three 
     * characters: "," or "|" or "&". 
     * 
     * String tag list string has the following format: 
     * <pre>
     *          java, j2ee, jdbc,...
     *       or java|j2ee|jdbc|...
     *       or java&j2ee&jdbc&...
     * </pre>
     * 
     * @param record the record to be tagged
     * @param tags a string of tags separated by comma
     * @return instance of AssociatedRecords
     */
    public static AssociatedRecords tagWith(ActiveRecord record, String tags) {
        if (tags == null || "".equals(tags)) return null;
        tags = tags.toLowerCase();
        
        List tagList = Converters.convertStringToList(tags);
        if (tagList == null) return null;
        
        //get all current tags of the record
        Set currentTags = allTags(record);

        ActiveRecord tagHome = ActiveRecordUtil.getHomeInstance(Tag.class);
        List tagRecords = new ArrayList();
        Iterator it = tagList.iterator();
        while(it.hasNext()) {
            String tagName = (String)it.next();
            if (currentTags != null && currentTags.contains(tagName)) continue;
            currentTags.add(tagName);
            
            ActiveRecord atag = tagHome.findFirst("name='" + tagName + "'");
            if (atag == null) {
                atag = new Tag();
                atag.setData("name=" + tagName);
                atag.create();
            }
            tagRecords.add(atag);
        }
        
        return record.allAssociated(Tag.class).add(tagRecords);
    }
    
    /**
     * Returns all tag records as specified by the tags. If a tag is not in 
     * the tags table, it is ignored.
     * 
     * @param tags the tags string
     * @return list of tag records
     */
    public static List findTagRecords(String tags) {
        if (tags == null || "".equals(tags)) return null;
        tags = tags.toLowerCase();
        
        List tagList = Converters.convertStringToList(tags);
        if (tagList == null) return null;

        ActiveRecord tagHome = ActiveRecordUtil.getHomeInstance(Tag.class);
        List tagRecords = new ArrayList();
        Iterator it = tagList.iterator();
        while(it.hasNext()) {
            String tagName = (String)it.next();
            ActiveRecord atag = tagHome.findFirst("name='" + tagName + "'");
            if (atag != null) {
                tagRecords.add(atag);
            }
        }
        
        return tagRecords;
    }
    
    /**
     * Returns all tag records as specified by the tags. If a tag is not in 
     * the tags table, it is added to the table.
     * 
     * @param tags the tags string
     * @return list of tag records
     */
    public static List findOrCreateTagRecords(String tags) {
        if (tags == null || "".equals(tags)) return null;
        tags = tags.toLowerCase();
        
        List tagList = Converters.convertStringToList(tags);
        if (tagList == null) return null;

        ActiveRecord tagHome = ActiveRecordUtil.getHomeInstance(Tag.class);
        List tagRecords = new ArrayList();
        Iterator it = tagList.iterator();
        while(it.hasNext()) {
            String tagName = (String)it.next();
            ActiveRecord atag = tagHome.findFirst("name='" + tagName + "'");
            if (atag == null) {
                atag = new Tag();
                atag.setData("name=" + tagName);
                atag.create();
            }
            tagRecords.add(atag);
        }
        
        return tagRecords;
    }
    
    /**
     * Returns records tagged with tags.
     * 
     * @param tags the tags condition
     * @return records tagged with the tags.
     */
    public static List findRecordsTaggedWith(String tags) {
        List tagRecords = findTagRecords(tags);
        if (tagRecords == null) return null;
        
        List records = new ArrayList();
        Iterator it = tagRecords.iterator();
        while(it.hasNext()) {
            Tag tag = (Tag)it.next();
            List list = tag.allAssociatedInCategory("taggable").getRecords();
            if (list != null) records.addAll(list);
        }
        
        return records;
    }
    
    /**
     * Returns records of a certain type that are tagged with tags.
     * 
     * @param type the specific record type
     * @param tags the tags condition
     * @return records tagged with the tags.
     */
    public static List findRecordsTaggedWith(String type, String tags) {
        List tagRecords = findTagRecords(tags);
        if (tagRecords == null) return null;
        
        List records = new ArrayList();
        Iterator it = tagRecords.iterator();
        while(it.hasNext()) {
            Tag tag = (Tag)it.next();
            List list = tag.allAssociatedInCategory("taggable", type).getRecords();
            if (list != null) records.addAll(list);
        }
        
        return records;
    }
    
    /**
     * Removes all tag records as specified by the tags string.
     * 
     * @param tags the tags string
     */
    public static void removeTags(String tags) {
        if (tags == null || "".equals(tags)) return;
        tags = tags.toLowerCase();
        
        List tagList = Converters.convertStringToList(tags);
        if (tagList == null) return;
        
        ActiveRecord tagHome = ActiveRecordUtil.getHomeInstance(Tag.class);
        Iterator it = tagList.iterator();
        while(it.hasNext()) {
            String tagName = (String)it.next();
            ActiveRecord atag = tagHome.findFirst("name='" + tagName + "'");
            if (atag != null) {
                atag.delete();
            }
        }
    }
    
    /**
     * Removes all tag records as specified by the tags string for a record.
     * 
     * @param tags the tags string
     */
    public static void removeRecordTags(ActiveRecord record, String tags) {
        if (tags == null || "".equals(tags)) return;
        tags = tags.toLowerCase();
        
        List tagNames = Converters.convertStringToList(tags);
        if (tagNames == null) return;
        
        List tagRecords = record.allAssociated(Tag.class).getRecords();
        if (tagRecords == null) return;
        
        Iterator it = tagRecords.iterator();
        while(it.hasNext()) {
            ActiveRecord tag = (ActiveRecord)it.next();
            String tagName = (String)tag.getField("name");
            if (tagNames.contains(tagName)) tag.delete();
        }
    }
}
