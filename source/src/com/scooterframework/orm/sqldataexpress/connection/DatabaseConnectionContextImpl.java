/*
 *   This software is distributed under the terms of the FSF 
 *   Gnu Lesser General Public License (see lgpl.txt). 
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.orm.sqldataexpress.connection;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Properties;

import com.scooterframework.common.util.Util;
import com.scooterframework.orm.sqldataexpress.config.DatabaseConfig;

/**
 * DataSourceConnectionContext class
 * 
 * @author (Fei) John Chen
 */
abstract public class DatabaseConnectionContextImpl implements DatabaseConnectionContext {
    /**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = -7091244526357697263L;

	public DatabaseConnectionContextImpl() {}
    
    public DatabaseConnectionContextImpl(Properties prop) {
        if (prop == null) throw new IllegalArgumentException("Database connection context properties is null.");
        this.properties = prop;
        init(prop);
    }
    
    /**
     * Returns database connection name
     *
     * @return String
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * Returns before-connection callback class.
     */
    public String getBeforeConnectionClassName() {
        return beforeConnectionClassName;
    }

    /**
     * Returns before-connection callback method.
     */
    public String getBeforeConnectionMethodName() {
        return beforeConnectionMethodName;
    }

    /**
     * Returns after-connection callback class.
     */
    public String getAfterConnectionClassName() {
        return afterConnectionClassName;
    }

    /**
     * Returns after-connection callback method.
     */
    public String getAfterConnectionMethodName() {
        return afterConnectionMethodName;
    }

    /**
     * Returns database username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns database password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the maximum time in seconds that this data source can wait while 
     * attempting to connect to a database
     *
     * @return Integer
     */
    public Integer getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * Sets the loginTimeout name of the database
     */
    public void setLoginTimeout(Integer loginTimeout) {
        this.loginTimeout = loginTimeout;
    }
    
    /**
     * Checks if the connection is readonly.
     *
     * @return true if readonly connection
     */
    public boolean isReadonly() {
        return readonly;
    }
    
    /**
     * Sets readonly connection
     *
     * @param readonly <tt>true</tt> if read only connection is desired
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
    
    /**
     * Returns the vendor name of the database
     *
     * @return String
     */
    public String getVendor() {
        return vendor;
    }
    
    /**
     * Checks if transaction isolation level is specified. If not, the 
     * database's default transaction isolation level is used.
     * 
     * @return true if specified
     */
    public boolean hasSpecifiedTransactionIsolationLevel() {
        return (transactionIsolationLevel != NO_SPECIFIED_TRANSACTION_ISOLATION)?true:false;
    }
    
    /**
     * Returns the specified transaction isolation level.
     *
     * @return String
     */
    public int getTransactionIsolationLevel() {
        return transactionIsolationLevel;
    }
    
    /**
     * Returns all database connection properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets all database connection properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    
    /**
     * Returns connection role name and password properties
     */
    public Properties getConnectionRoles() {
        return roles;
    }
    
    /**
     * Sets connection role name and password properties
     */
    public void setConnectionRoles(Properties roles) {
        this.roles = roles;
    }
    
    /**
     * <p>Help method to detect vendor from <tt>input</tt> string. The input 
     * string could be one of the following:</p>
     * 
     * <ul>
     *   <li>driverClassName</li>
     *   <li>url</li>
     *   <li>connectionName</li>
     * </ul>
     */
    protected String checkVendor(String input) {
        if (input == null) return null;
        
        String theVendor = null;
        
        Iterator it = DatabaseConfig.ALL_BUILTIN_DATABASE_VENDORS.iterator();
        while(it.hasNext()){
            String vendor = (String)it.next();
            if (input.toUpperCase().indexOf(vendor.toUpperCase()) != -1) {
                theVendor = vendor;
                break;
            }
        }
        
        return theVendor;
    }

    /**
     * initializes database connection context
     */
    private void init(Properties prop) {
        connectionName = prop.getProperty(DatabaseConnectionContext.KEY_CONNECTION_NAME);
        if (connectionName == null) throw new IllegalArgumentException("Database connection name not found in " + prop);
        
        username = prop.getProperty(DatabaseConnectionContext.KEY_USERNAME);
        password = prop.getProperty(DatabaseConnectionContext.KEY_PASSWORD);
        vendor = prop.getProperty(DatabaseConnectionContext.KEY_VENDOR);
        readonly = ("true".equalsIgnoreCase(prop.getProperty(DatabaseConnectionContext.KEY_READONLY)))?true:false;
        
        String til = prop.getProperty(DatabaseConnectionContext.KEY_TRANSACTION_ISOLATION_LEVEL);
        if (til != null) {
            //verify transaction isolation level
            int tilLevel = Util.getSafeIntValue(til);
            if (tilLevel != Connection.TRANSACTION_NONE && 
                tilLevel != Connection.TRANSACTION_READ_UNCOMMITTED && 
                tilLevel != Connection.TRANSACTION_READ_UNCOMMITTED && 
                tilLevel != Connection.TRANSACTION_READ_UNCOMMITTED && 
                tilLevel != Connection.TRANSACTION_READ_UNCOMMITTED) {
                throw new IllegalArgumentException("Transaction isolation level specified is not valid: \"" + tilLevel + "\".");
            }
            else {
                transactionIsolationLevel = tilLevel;
            }
        }
        else {
            transactionIsolationLevel = NO_SPECIFIED_TRANSACTION_ISOLATION;
        }
        
        String loginTimeoutStr = prop.getProperty(DatabaseConnectionContext.KEY_LOGINTIMEOUT);
        if (loginTimeoutStr != null) {
            try {
                loginTimeout = new Integer(loginTimeoutStr);
            }
            catch(Exception ex) {
                throw new IllegalArgumentException("Failed to parse login timeout string: " + loginTimeoutStr);
            }
        }
        
        String beforeConnection = prop.getProperty(DatabaseConnectionContext.KEY_BEFORE_CONNECTION);
        if (beforeConnection != null) {
            try {
                int lastDot = beforeConnection.lastIndexOf('.');
                beforeConnectionClassName = beforeConnection.substring(0, lastDot);
                beforeConnectionMethodName = beforeConnection.substring(lastDot + 1);
            }
            catch(Exception ex) {
                throw new IllegalArgumentException("Failed to parse before connection: " + beforeConnection);
            }
        }
        
        String afterConnection = prop.getProperty(DatabaseConnectionContext.KEY_AFTER_CONNECTION);
        if (afterConnection != null) {
            try {
                int lastDot = beforeConnection.lastIndexOf('.');
                afterConnectionClassName = beforeConnection.substring(0, lastDot);
                afterConnectionMethodName = beforeConnection.substring(lastDot + 1);
            }
            catch(Exception ex) {
                throw new IllegalArgumentException("Failed to parse after connection: " + beforeConnection);
            }
        }
    }

    protected String connectionName = null;
    protected String beforeConnectionClassName = null;
    protected String beforeConnectionMethodName = null;
    protected String afterConnectionClassName = null;
    protected String afterConnectionMethodName = null;
    protected String username = null;
    protected String password = null;
    protected Integer loginTimeout = null;
    protected boolean readonly = false;
    protected int transactionIsolationLevel = NO_SPECIFIED_TRANSACTION_ISOLATION;
    protected String vendor = null;
    protected Properties properties = new Properties();
    protected Properties roles = new Properties();
    
    private static final int NO_SPECIFIED_TRANSACTION_ISOLATION = -1;
}
