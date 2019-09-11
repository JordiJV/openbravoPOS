//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.sync;

import java.util.List;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.*;
import com.openbravo.data.model.Field;
import com.openbravo.data.model.Row;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.BeanFactoryDataSingle;

/**
 *
 * @author jjuanmarti
 */
public class DataLogicSync extends BeanFactoryDataSingle {

    protected Session s;
    Row row;

    /** Creates a new instance of SentenceContainerGeneric */
    public DataLogicSync() {

        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING)
                //new Field("PRODUCT_ID", Datas.STRING, Formats.STRING),
                //new Field(AppLocal.getIntString("label.prodref"), Datas.STRING, Formats.STRING, true, true, true),
                //new Field(AppLocal.getIntString("label.prodname"), Datas.STRING, Formats.STRING, true, true, true),
                //new Field("LOCATION", Datas.STRING, Formats.STRING),
                //new Field("STOCKSECURITY", Datas.DOUBLE, Formats.DOUBLE),
                //new Field("STOCKMAXIMUM", Datas.DOUBLE, Formats.DOUBLE)
                //new Field("UNITS", Datas.DOUBLE, Formats.DOUBLE)
        );
    }

    public void init(Session s){
        this.s = s;
    }
    
    public List getStockLinesPending() throws BasicException {
        return new PreparedSentence(s
                , "select sto.ID, sto.DATENEW, pro.REFERENCE, pro.NAME, sto.UNITS, sto.IS_PS_SYNC " +
                  "from stockdiary sto " +
                  "join products pro on pro.ID=sto.PRODUCT " +
                  "where sto.IS_PS_SYNC = 0 and pro.reference != '500' and pro.reference != '501'" +
                  "order by sto.datenew desc"
                , null
                , StockDiarySync.getSerializerRead()).list();
    }
   
    public final SentenceExec getStockSyncToUpdate() {
        return new SentenceExecTransaction(s) {
            public int execInTransaction(Object params) throws BasicException {
            	Object[] values = (Object[]) params;
            	// UPDATE
            	int updateresult = new PreparedSentence(s
                        , "UPDATE STOCKDIARY SET IS_PS_SYNC = 1 WHERE ID = ?"
                        , new SerializerWriteBasicExt(row.getDatas(), new int[] {0})).exec(params);

                return updateresult;
            }
        };
    }

    public final Long getCurrentMonthClosedCash () throws BasicException {

        return (Long) new PreparedSentence(s,
                "SELECT SUM(p.TOTAL) FROM CLOSEDCASH c JOIN RECEIPTS r ON r.MONEY=c.MONEY JOIN PAYMENTS p ON p.RECEIPT=r.ID WHERE YEAR(c.DATEEND)=YEAR(CURDATE()) AND MONTH(c.DATEEND)=MONTH(CURDATE())",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE).find();

    }
    
 }
