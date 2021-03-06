/*
 *   This software is distributed under the terms of the FSF 
 *   Gnu Lesser General Public License (see lgpl.txt). 
 *
 *   This program is distributed WITHOUT ANY WARRANTY. See the
 *   GNU General Public License for more details.
 */
package com.scooterframework.orm.sqldataexpress.processor;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.scooterframework.common.logging.LogUtil;
import com.scooterframework.common.util.StringUtil;
import com.scooterframework.common.util.Util;
import com.scooterframework.orm.sqldataexpress.connection.UserDatabaseConnection;
import com.scooterframework.orm.sqldataexpress.exception.BaseSQLException;
import com.scooterframework.orm.sqldataexpress.object.Cursor;
import com.scooterframework.orm.sqldataexpress.object.JdbcStatement;
import com.scooterframework.orm.sqldataexpress.object.OmniDTO;
import com.scooterframework.orm.sqldataexpress.object.Parameter;
import com.scooterframework.orm.sqldataexpress.object.RowData;
import com.scooterframework.orm.sqldataexpress.object.RowInfo;
import com.scooterframework.orm.sqldataexpress.object.TableData;
import com.scooterframework.orm.sqldataexpress.parser.JdbcStatementParser;
import com.scooterframework.orm.sqldataexpress.parser.ParameterMetaDataLoader;
import com.scooterframework.orm.sqldataexpress.util.DAOUtil;
import com.scooterframework.orm.sqldataexpress.util.DBStore;
import com.scooterframework.orm.sqldataexpress.util.SqlExpressUtil;
import com.scooterframework.orm.sqldataexpress.util.SqlUtil;
import com.scooterframework.orm.sqldataexpress.vendor.DBAdapter;
import com.scooterframework.orm.sqldataexpress.vendor.DBAdapterFactory;


/**
 * JdbcStatementProcessor class.
 * 
 * @author (Fei) John Chen
 */
public class JdbcStatementProcessor extends DataProcessorImpl {
    private JdbcStatement st = null;
    
    public JdbcStatementProcessor(JdbcStatement st) {
        this.st = st;
    }

    
    /**
     * execute with output filter
     */
    public OmniDTO execute(UserDatabaseConnection udc, Map inputs, Map outputFilters) 
    throws BaseSQLException {
    	Connection connection = udc.getConnection();
    	DBAdapter dba = DBAdapterFactory.getInstance().getAdapter(udc.getConnectionName());
    	
        OmniDTO returnTO = new OmniDTO();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        //log.debug("execute - inputs: " + inputs);
        //log.debug("execute - outputFilters: " + outputFilters);
        
        try {
            String stName = st.getName();
            autoFill(udc, inputs);
            
            String originalSql = st.getOriginalJdbcStatementString();
            if(checkPagination(inputs)) {
            	outputFilters = new HashMap();
                String updatedOriginalSql = dba.preparePaginationSql(originalSql, inputs, outputFilters);
                st = updateStatement(updatedOriginalSql);
            }
            
            String executableSql = st.getExecutableJdbcStatementString();
            executableSql = autoReplace(executableSql, inputs);
            
            log.debug("execute - parsed expecutable sql: " + executableSql);
            log.debug("execute - parsed inputs: " + inputs);
            log.debug("execute - outputFilters: " + outputFilters);
            
            boolean supportsGetGeneratedKeys = supportsGetGeneratedKeys();
            if (supportsGetGeneratedKeys) {
                pstmt = connection.prepareStatement(executableSql, Statement.RETURN_GENERATED_KEYS);
            }
            else {
                pstmt = connection.prepareStatement(executableSql);
            }
            
            // check if need to load parameter properties
            if (supportParameterMetaData()) {
                if (!st.hasLoadedParameterMetaData()) {
                    //get parameter meta data if it has not been loaded
                    ParameterMetaData pmd = pstmt.getParameterMetaData();
                    ParameterMetaDataLoader pmdl = new ParameterMetaDataLoader(pmd, st);
                    pmdl.loadParameterMetaData(pmd);
                }
            }
            else {
                if (!st.hasLoadedParameterProperties()) {
                    JdbcStatementParser parser = new JdbcStatementParser(udc, st);
                    parser.parse();
                }
            }
            
            Collection parameters = st.getParameters();
            log.debug("execute - parameters: " + parameters);
            Iterator pit = parameters.iterator();
            while(pit.hasNext()) {
                Parameter p = (Parameter) pit.next();
                
                String key = p.getName();
                if (!inputs.containsKey(key)) {
                	throw new Exception("There " + 
                    "must be a key/value pair corresponding to key named " + key + 
                    " in input parameters: " + inputs.keySet());
                }
                
                if (Parameter.MODE_IN.equals(p.getMode())) {
                    Object obj = inputs.get(key);
                    if (obj == null || 
                        "".equals(obj.toString().trim()) && 
                        p.getSqlDataType() != Types.CHAR && 
                        p.getSqlDataType() != Types.VARCHAR && 
                        p.getSqlDataType() != Types.LONGVARCHAR) {
                        setNull(pstmt, p.getIndex(), p.getSqlDataType());
                    }
                    else {
                        if(!dba.vendorSpecificSetObject(pstmt, obj, p, inputs)) {
                            if (Parameter.UNKNOWN_SQL_DATA_TYPE != p.getSqlDataType()) {
                                setObject(pstmt, obj, p);
                            }
                            else {
                                //It is up to JDBC driver's PreparedStatement implementation 
                                //class to deal with. Usually the class will make a decision 
                                //on which setXXX(Type) method to call based on the java 
                                //class type of the obj instance. 
                                pstmt.setObject(p.getIndex(), obj);
                            }
                        }
                    }
                }
            }
            
            if (st.isSelectStatement()) {
                rs = pstmt.executeQuery();
                
                // handle out cursors or other outputs if there is any
                if (rs != null) {
                    if (outputFilters == null) {
                        handleResultSet(dba, stName, returnTO, rs, inputs);
                    }
                    else {
                        handleFilteredResultSet(dba, stName, returnTO, rs, inputs, outputFilters);
                    }
                }
            }
            else {
                int rowCount = pstmt.executeUpdate();
                returnTO.setUpdatedRowCount(rowCount);
                
                //get generated key if the underline database permitted
                if (supportsGetGeneratedKeys) {
                    ResultSet rsg = null;
                    try {
                        rsg = pstmt.getGeneratedKeys();
                        if(rsg.next()) {
                            returnTO.setGeneratedKey(rsg.getLong(1));
                        }
                    }
                    catch(Throwable ex) {
                        DAOUtil.closeResultSet(rsg);
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new BaseSQLException(ex);
        }
        finally {
            DAOUtil.closeResultSet(rs);
            DAOUtil.closeStatement(pstmt);
        }
        
        return returnTO;
    }

    protected boolean checkPagination(Map inputs) {
        boolean usePagination = false;
        if(st.isSelectStatement()) {
            usePagination = Util.getBooleanValue(inputs, DataProcessor.input_key_use_pagination, false);
            if (!usePagination) {
                int limit = Util.getIntValue(inputs, DataProcessor.input_key_records_limit, DataProcessor.NO_ROW_LIMIT);
                boolean requireFixed = Util.getBooleanValue(inputs, DataProcessor.input_key_records_fixed, false);
                if (limit != DataProcessor.NO_ROW_LIMIT && limit > 0 && !requireFixed) {
                    usePagination = true;
                }
            }
        }
        
        return usePagination;
    }


    //auto fill some values
    private void autoFill(UserDatabaseConnection udc, Map inputs) {
        String jdbcStatementString = st.getOriginalJdbcStatementString();
        
        if (jdbcStatementString.indexOf("?@") == -1) return;//nothing to fill
        
        StringTokenizer sti = new StringTokenizer(jdbcStatementString, " ,><=(){}");
        while(sti.hasMoreTokens()) 
        {
            String token = sti.nextToken();
            
            //replace all occurances of token by '?'
            if (token.length()>2 && token.startsWith("?@")) {
                String key = token.substring(2);
                
                DataProcessor dp = 
                    DataProcessorFactory.getInstance().getDataProcessor( 
                            udc, 
                            DataProcessorTypes.NAMED_SQL_STATEMENT_PROCESSOR, 
                            key);
                OmniDTO returnTO = dp.execute(udc, inputs);
                Object result = returnTO.getTableData(key).getFirstObject();
                
                log.debug("autoFill: result for key " + key + ": " + result);
                inputs.put("@"+key, result);
            }
        }
    }

    /**
     * Replaces some tokens in the sql string with data from input map. The 
     * parts that need to be replaced are parts in the sql string that start 
     * with SqlUtil.REPLACE_PART_START and end with SqlUtil.REPLACE_PART_END.
     * 
     * @param original
     * @param inputs
     * @return 
     */
    private String autoReplace(String original, Map inputs) {
        if (original.indexOf(SqlUtil.REPLACE_PART_START) == -1 &&
            original.indexOf(SqlUtil.REPLACE_PART_END) == -1) return original;
        String replaced = original;
        List replaceKeys = new ArrayList();
        StringTokenizer st = new StringTokenizer(original, " ,");
        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.startsWith(SqlUtil.REPLACE_PART_START) && token.endsWith(SqlUtil.REPLACE_PART_END)) {
                replaceKeys.add(token);
            }
        }
        
        for(Iterator it = replaceKeys.iterator(); it.hasNext();) {
            String token = (String)it.next();
            String key = token.substring(1, token.length()-1);
            String replaceStr = (String)inputs.get(key);
            if (replaceStr == null) throw new IllegalArgumentException("There is no input data to replace " + token + ".");
            replaced = StringUtil.replace(replaced, token, replaceStr);
        }
        
        return replaced;
    }

    private void handleResultSet(DBAdapter dba, String stName, OmniDTO returnTO, ResultSet rs, Map inputs) 
    throws SQLException {
        Cursor cursor = st.getCursor(stName, rs);
        int cursorWidth = cursor.getDimension();
        
        TableData rt = new TableData();
        rt.setHeader(cursor);
        returnTO.addTableData(stName, rt);
        
        while(rs.next()) {
            Object[] cellValues = new Object[cursorWidth];
            for (int i = 0; i < cursorWidth; i++) {
                cellValues[i] = dba.getObjectFromResultSetByType(rs, 
                                                             cursor.getColumnJavaClassName(i), 
                                                             cursor.getColumnSqlDataType(i),
                                                             i+1);
            }
            rt.addRow(new RowData(cursor, cellValues));
        }
        rs.close();
    }
    
    private void handleFilteredResultSet(DBAdapter dba, String stName, OmniDTO returnTO, ResultSet rs, Map inputs, Map outputs) 
    throws SQLException {
        Cursor cursor = st.getCursor(stName, rs);
        int cursorWidth = cursor.getDimension();
        
        Set allowedColumns = getAllowedColumns(outputs, cursor);
        TableData rt = new TableData();
        RowInfo newHeader = getFilteredHeaderInfo(allowedColumns, cursor);
        rt.setHeader(newHeader);
        returnTO.addTableData(stName, rt);
        
        while(rs.next()) {
            ArrayList cellValues = new ArrayList();
            for (int i = 0; i < cursorWidth; i++) {
                if (allowedColumns.contains(cursor.getColumnName(i))) {
                    cellValues.add(dba.getObjectFromResultSetByType(rs, 
                                                                cursor.getColumnJavaClassName(i), 
                                                                cursor.getColumnSqlDataType(i),
                                                                i+1));
                }
            }
            
            if (cellValues.size() > 0) 
                rt.addRow(new RowData(newHeader, cellValues.toArray()));
        }
        rs.close();
    }
    
    private JdbcStatement updateStatement(String processorName) {
        JdbcStatement statement = DBStore.getInstance().getJdbcStatement(processorName);
        
        if (statement == null) {
            //discovery
            statement = SqlExpressUtil.createJdbcStatementDirect(processorName);
            DBStore.getInstance().addJdbcStatement(processorName, statement);
        }
        
        return statement;
    }
    
    /**
     * Oracle doesn't support ParameterMetaData. 
     * MYSQL doesn't fully support ParameterMetaData.
     * 
     */
    protected boolean supportParameterMetaData() {
        return false;
    }
    
    protected LogUtil log = LogUtil.getLogger(this.getClass().getName());
}
