package at.ac.tuwien.inso.subcat.model;

import java.sql.SQLException;


public abstract class ObjectCallback<T> {
	
	public abstract boolean processResult (T item) throws SQLException, Exception;
}
