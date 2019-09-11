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

import java.util.Date;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.SerializerRead;

/**
 * 
 * @author jjuanmarti
 * 
 */
public class StockDiarySync {

	private static final long serialVersionUID = 7587696873036L;

	protected String id;
	protected Date datenew;
	protected String productRef;
	protected String productName;
	protected int units;
	protected boolean isPSSync;

	/** Creates new StockDiarySync */
	public StockDiarySync() {
		id = null;
		units = 0;
		isPSSync = false;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDatenew() {
		return datenew;
	}

	public void setDatenew(Date datenew) {
		this.datenew = datenew;
	}

	public String getProductRef() {
		return productRef;
	}

	public void setProductRef(String productRef) {
		this.productRef = productRef;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public boolean isPSSync() {
		return isPSSync;
	}

	public void setPSSync(boolean isPSSync) {
		this.isPSSync = isPSSync;
	}

	public static SerializerRead getSerializerRead() {
		return new SerializerRead() {
			public Object readValues(DataRead dr) throws BasicException {
				StockDiarySync stockSync = new StockDiarySync();
				stockSync.id = dr.getString(1);
				stockSync.datenew = dr.getTimestamp(2);
				stockSync.productRef = dr.getString(3);
				stockSync.productName = dr.getString(4);
				stockSync.units = ((Double)dr.getObject(5)).intValue();
				stockSync.isPSSync = dr.getBoolean(6).booleanValue();
				return stockSync;
			}
		};
	}

	@Override
	public final String toString() {
		return datenew + " - " + productRef + " - " + productName + " - " + units;
	}
}
