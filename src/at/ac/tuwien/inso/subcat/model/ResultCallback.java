package at.ac.tuwien.inso.subcat.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import at.ac.tuwien.inso.subcat.config.SemanticException;


public interface ResultCallback {

	public void processResult (ResultSet res) throws SemanticException, SQLException, Exception;
}
