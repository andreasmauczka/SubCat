package at.ac.tuwien.inso.subcat.ui.widgets;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import at.ac.tuwien.inso.subcat.model.User;
import at.ac.tuwien.inso.subcat.ui.events.UserListListener;


public class UserList extends Composite {
	private static class UserStatus {
		public boolean checked;
		public User user;

		public UserStatus (User user) {
			assert (user != null);

			this.checked = false;
			this.user = user;
		}
	}

	private LinkedList<UserListListener> listeners
		= new LinkedList<UserListListener> ();

	private List<UserStatus> users = new LinkedList<UserStatus> ();
	private Text text;
	private Table table;


	public UserList(Composite arg0, int arg1, List<User> users, final boolean enableMultiSelection) {
		super (arg0, arg1);
		setLayout (new GridLayout (1, false));
		
		text = new Text (this, SWT.BORDER | SWT.SEARCH);
		text.setLayoutData (new GridData (SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		table = new Table (this, SWT.BORDER | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | ((enableMultiSelection)? SWT.CHECK : SWT.SINGLE));
		table.setLayoutData (new GridData (SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible (false);
		table.setLinesVisible (true);
	
		TableColumn clmnName = new TableColumn (table, SWT.NONE);
		clmnName.setText ("Name");
		clmnName.pack ();
		
		setUsers (users);

		text.addModifyListener (new ModifyListener () {
			@Override
			public void modifyText (ModifyEvent arg0) {
				filter ();
			}
		});

		table.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event event) {
				if (enableMultiSelection && event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					UserStatus data = (UserStatus) item.getData ();
					data.checked = item.getChecked ();
					
					for (UserListListener listener : listeners) {
						listener.selectionChanged ();
					}
				} else if (!enableMultiSelection) {
					TableItem item = (TableItem) event.item;
					UserStatus data = (UserStatus) item.getData ();

					for (UserStatus user : UserList.this.users) {
						user.checked = (user.user.getId () == data.user.getId ());
					}

					for (UserListListener listener : listeners) {
						listener.selectionChanged ();
					}
				}
			}
		});
	}


	private void filter () {
		String[] _keys = text.getText ().split ("\\s+");
		
		table.setRedraw (false);
		table.removeAll ();

		for (UserStatus item : users) {
			LinkedList<String> keys = new LinkedList<String> ();
			Collections.addAll (keys, _keys);

			cmpConsume (item.user.getName (), keys);

			if (keys.size () == 0) {
				TableItem tableItem = new TableItem (table, SWT.NONE);
				tableItem.setChecked (item.checked);
				tableItem.setText (0, item.user.getName ());
				tableItem.setData (item);
			}
		}

		table.setRedraw (true);
	}

	private void cmpConsume (String str, LinkedList<String> keys) {
		assert (str != null);
		assert (keys != null);

		Iterator<String> iter = keys.iterator ();
		while (iter.hasNext ()) {
			if (StringUtils.containsIgnoreCase(str, iter.next ())) {
				iter.remove ();
			}
		}
	}
	
	public void setUsers (List<User> users) {
		assert (users != null);
		
		this.users = new LinkedList<UserStatus> ();
		for (User user : users) {
			this.users.add (new UserStatus (user));
		}
		filter ();
	}

	public List<Integer> getSelectedIDs () {
		LinkedList<Integer> ids = new LinkedList<Integer> ();
		for (UserStatus item : users) {
			if (item.checked == true) {
				Integer id = item.user.getId ();
				assert (id != null);
				ids.add (id);
			}
		}

		return ids;
	}

	public void addUserListListener (UserListListener listener) {
		assert (listener != null);

		this.listeners.add (listener);
	}

	/*
	public static void main (String[] args) {
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout (new FillLayout ());

		new UserList (shell, SWT.NONE);
		
	    shell.open ();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) {
				display.sleep ();
			}
		}
		
		display.dispose();		
	}
	*/
}
