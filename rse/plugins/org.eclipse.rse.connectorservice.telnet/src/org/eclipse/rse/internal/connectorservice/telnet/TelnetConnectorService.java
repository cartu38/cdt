/*******************************************************************************
 * Copyright (c) 2006, 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Martin Oberhuber (Wind River) - initial API and implementation
 * David Dykstal (IBM) - 168977: refactoring IConnectorService and ServerLauncher hierarchies
 * Sheldon D'souza (Celunite) - adapted from SshConnectorService
 *******************************************************************************/
package org.eclipse.rse.internal.connectorservice.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;

import org.apache.commons.net.telnet.TelnetClient;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.rse.core.SystemBasePlugin;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.model.SystemSignonInformation;
import org.eclipse.rse.core.subsystems.AbstractConnectorService;
import org.eclipse.rse.core.subsystems.CommunicationsEvent;
import org.eclipse.rse.core.subsystems.IConnectorService;
import org.eclipse.rse.core.subsystems.ICredentialsProvider;
import org.eclipse.rse.core.subsystems.SubSystemConfiguration;
import org.eclipse.rse.internal.services.telnet.ITelnetSessionProvider;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.eclipse.rse.ui.subsystems.StandardCredentialsProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class TelnetConnectorService extends AbstractConnectorService implements ITelnetSessionProvider  {
	
	private static final int TELNET_DEFAULT_PORT = 23;
	private static final int CONNECT_DEFAULT_TIMEOUT = 60; //seconds
	private static TelnetClient fTelnetClient = new TelnetClient();
	private SessionLostHandler fSessionLostHandler;
	private InputStream in;
    private PrintStream out;
    private static final String PROMPT = "$"; //$NON-NLS-1$
    private static final String ARM_PROMT = "#"; //$NON-NLS-1$
    private static final boolean arm_flag = true;
	private ICredentialsProvider credentialsProvider = null;
	
	public TelnetConnectorService(IHost host) {
		super(TelnetConnectorResources.TelnetConnectorService_Name, TelnetConnectorResources.TelnetConnectorService_Description, host, 0);
		fSessionLostHandler = null;
	}

	public static void checkCanceled(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			throw new OperationCanceledException();
	}
	
		
	protected void internalConnect(IProgressMonitor monitor) throws Exception {
		
		
		String host = getHostName();
	    String user = getUserId();
	    String password = ""; //$NON-NLS-1$
       
        try {
        	Activator.trace("Telnet Service: Connecting....."); //$NON-NLS-1$
        	fTelnetClient.connect(host,TELNET_DEFAULT_PORT );
        	SystemSignonInformation ssi = getPasswordInformation();
            if (ssi!=null) {
            	password = getPasswordInformation().getPassword();
            }
            
            in = fTelnetClient.getInputStream();
            out = new PrintStream( fTelnetClient.getOutputStream() );
            if( !arm_flag ) {
	            readUntil( "login: "); //$NON-NLS-1$
	            write( user );
	            readUntil( "Password: "); //$NON-NLS-1$
	            write( password );
	            readUntil( PROMPT );
            }else {
            	readUntil( ARM_PROMT );
            }
        	Activator.trace("Telnet Service: Connected"); //$NON-NLS-1$
        }catch( SocketException se) {
        	Activator.trace("Telnet Service failed: "+se.toString()); //$NON-NLS-1$
            if (fTelnetClient.isConnected())
            	fTelnetClient.disconnect();
        }catch( IOException ioe ) {
        	Activator.trace("Telnet Service failed: "+ioe.toString()); //$NON-NLS-1$
        	 if (fTelnetClient.isConnected())
             	fTelnetClient.disconnect();
        }
        
		fSessionLostHandler = new SessionLostHandler( this );
		notifyConnection();
		
	}
	
	public String readUntil( String pattern ) {
		   try {
			 char lastChar = pattern.charAt( pattern.length() - 1 );
			 StringBuffer sb = new StringBuffer();
			 boolean found = false;
			 char ch = ( char )in.read();
			 while( true ) {
			  System.out.print( ch );
			  sb.append( ch );
			  if( ch == lastChar ) {
			    if( sb.toString().endsWith( pattern ) ) {
				 return sb.toString();
			    }
			  }
			  ch = ( char )in.read();
			 }
		   }
		   catch( Exception e ) {
			 e.printStackTrace();
		   }
		   return null;
	}

	public void write( String value ) {
	   try {
		 out.println( value );
		 out.flush();
		 Activator.trace("write: "+value ); //$NON-NLS-1$
	   }
	   catch( Exception e ) {
		 e.printStackTrace();
	   }
	}
					 
	protected void internalDisconnect(IProgressMonitor monitor) throws Exception {
		
		Activator.trace("Telnet Service: Disconnecting ....."); //$NON-NLS-1$
		
		
		boolean sessionLost = (fSessionLostHandler!=null && fSessionLostHandler.isSessionLost());
		// no more interested in handling session-lost, since we are disconnecting anyway
		fSessionLostHandler = null;
		// handle events
		if (sessionLost) {
			notifyError();
		} 
		else {
			// Fire comm event to signal state about to change
			fireCommunicationsEvent(CommunicationsEvent.BEFORE_DISCONNECT);
		}

		if( fTelnetClient.isConnected() ) {
			fTelnetClient.disconnect();
		}
		
		// Fire comm event to signal state changed
		notifyDisconnection();
	}
	
	protected ICredentialsProvider getCredentialsProvider() {
		if (credentialsProvider == null) {
			credentialsProvider = new StandardCredentialsProvider(this);
		}
		return credentialsProvider;
	}
	
	public TelnetClient getTelnetClient() {
		return fTelnetClient;
	}

	/**
     * Handle session-lost events.
     * This is generic for any sort of connector service.
     * Most of this is extracted from dstore's ConnectionStatusListener.
     * 
     * TODO should be refactored to make it generally available, and allow
     * dstore to derive from it.
     */
	public static class SessionLostHandler implements Runnable, IRunnableWithProgress
	{
		private IConnectorService _connection;
		private boolean fSessionLost;
		
		public SessionLostHandler(IConnectorService cs)
		{
			_connection = cs;
			fSessionLost = false;
		}
		
		/** 
		 * Notify that the connection has been lost. This may be called 
		 * multiple times from multiple subsystems. The SessionLostHandler
		 * ensures that actual user feedback and disconnect actions are
		 * done only once, on the first invocation.
		 */
		public void sessionLost()
		{
			//avoid duplicate execution of sessionLost
			boolean showSessionLostDlg=false;
			synchronized(this) {
				if (!fSessionLost) {
					fSessionLost = true;
					showSessionLostDlg=true;
				}
			}
			if (showSessionLostDlg) {
				//invokes this.run() on dispatch thread
				Display.getDefault().asyncExec(this);
			}
		}
		
		public synchronized boolean isSessionLost() {
			return fSessionLost;
		}
		
		public void run()
		{
			Shell shell = getShell();
			//TODO need a more correct message for "session lost"
			//TODO allow users to reconnect from this dialog
			//SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_UNKNOWNHOST);
			SystemMessage msg = RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_CONNECT_CANCELLED);
			msg.makeSubstitution(_connection.getPrimarySubSystem().getHost().getAliasName());
			SystemMessageDialog dialog = new SystemMessageDialog(getShell(), msg);
			dialog.open();
			try
			{
				//TODO I think we should better use a Job for disconnecting?
				//But what about error messages?
				IRunnableContext runnableContext = getRunnableContext(getShell());
				// will do this.run(IProgressMonitor mon)
		    	//runnableContext.run(false,true,this); // inthread, cancellable, IRunnableWithProgress
		    	runnableContext.run(true,true,this); // fork, cancellable, IRunnableWithProgress
		    	_connection.reset();
				ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();    	    
	            sr.connectedStatusChange(_connection.getPrimarySubSystem(), false, true, true);
			}
	    	catch (InterruptedException exc) // user cancelled
	    	{
	    	  if (shell != null)    		
	            showDisconnectCancelledMessage(shell, _connection.getHostName(), _connection.getPort());
	    	}    	
	    	catch (java.lang.reflect.InvocationTargetException invokeExc) // unexpected error
	    	{
	    	  Exception exc = (Exception)invokeExc.getTargetException();
	    	  if (shell != null)    		
	    	    showDisconnectErrorMessage(shell, _connection.getHostName(), _connection.getPort(), exc);    	    	
	    	}
			catch (Exception e)
			{
				SystemBasePlugin.logError(TelnetConnectorResources.TelnetConnectorService_ErrorDisconnecting, e);
			}
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException
		{
			String message = null;
			message = SubSystemConfiguration.getDisconnectingMessage(
					_connection.getHostName(), _connection.getPort());
			monitor.beginTask(message, IProgressMonitor.UNKNOWN);
			try {
				_connection.disconnect(monitor);
			} catch (Exception exc) {
				if (exc instanceof java.lang.reflect.InvocationTargetException)
					throw (java.lang.reflect.InvocationTargetException) exc;
				if (exc instanceof java.lang.InterruptedException)
					throw (java.lang.InterruptedException) exc;
				throw new java.lang.reflect.InvocationTargetException(exc);
			} finally {
				monitor.done();
			}
		}

		public Shell getShell() {
			Shell activeShell = SystemBasePlugin.getActiveWorkbenchShell();
			if (activeShell != null) {
				return activeShell;
			}

			IWorkbenchWindow window = null;
			try {
				window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			} catch (Exception e) {
				return null;
			}
			if (window == null) {
				IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
						.getWorkbenchWindows();
				if (windows != null && windows.length > 0) {
					return windows[0].getShell();
				}
			} else {
				return window.getShell();
			}

			return null;
		}

	    /**
		 * Get the progress monitor dialog for this operation. We try to use one
		 * for all phases of a single operation, such as connecting and
		 * resolving.
		 */
		protected IRunnableContext getRunnableContext(Shell rshell) {
			Shell shell = getShell();
			// for other cases, use statusbar
			IWorkbenchWindow win = SystemBasePlugin.getActiveWorkbenchWindow();
			if (win != null) {
				Shell winShell = RSEUIPlugin.getDefault().getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				if (winShell != null && !winShell.isDisposed()
						&& winShell.isVisible()) {
					SystemBasePlugin
							.logInfo("Using active workbench window as runnable context"); //$NON-NLS-1$
					shell = winShell;
					return win;
				} else {
					win = null;
				}
			}
			if (shell == null || shell.isDisposed() || !shell.isVisible()) {
				SystemBasePlugin
						.logInfo("Using progress monitor dialog with given shell as parent"); //$NON-NLS-1$
				shell = rshell;
			}
			IRunnableContext dlg = new ProgressMonitorDialog(rshell);
			return dlg;
		}

	    /**
		 * Show an error message when the disconnection fails. Shows a common
		 * message by default. Overridable.
		 */
	    protected void showDisconnectErrorMessage(Shell shell, String hostName, int port, Exception exc)
	    {
	         //SystemMessage.displayMessage(SystemMessage.MSGTYPE_ERROR,shell,RSEUIPlugin.getResourceBundle(),
	         //                             ISystemMessages.MSG_DISCONNECT_FAILED,
	         //                             hostName, exc.getMessage()); 	
	         //RSEUIPlugin.logError("Disconnect failed",exc); // temporary
	    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell,
	    	            RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_DISCONNECT_FAILED).makeSubstitution(hostName,exc));
	    	 msgDlg.setException(exc);
	    	 msgDlg.open();
	    }	

	    /**
	     * Show an error message when the user cancels the disconnection.
	     * Shows a common message by default.
	     * Overridable.
	     */
	    protected void showDisconnectCancelledMessage(Shell shell, String hostName, int port)
	    {
	         //SystemMessage.displayMessage(SystemMessage.MSGTYPE_ERROR, shell, RSEUIPlugin.getResourceBundle(),
	         //                             ISystemMessages.MSG_DISCONNECT_CANCELLED, hostName);
	    	 SystemMessageDialog msgDlg = new SystemMessageDialog(shell,
	    	            RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_DISCONNECT_CANCELLED).makeSubstitution(hostName));
	    	 msgDlg.open();
	    }
	}

	/* Notification from sub-services that our session was lost.
    * Notify all subsystems properly.
    * TODO allow user to try and reconnect?
    */
	public void handleSessionLost() {
	   	Activator.trace("TelnetConnectorService: handleSessionLost"); //$NON-NLS-1$
	   	if (fSessionLostHandler!=null) {
	   		fSessionLostHandler.sessionLost();
	   	}
	}

	protected static Display getStandardDisplay() {
	   	Display display = Display.getCurrent();
	   	if( display==null ) {
	   		display = Display.getDefault();
	   	}
	   	return display;
	}
	
	public boolean isConnected() {
		if (fTelnetClient.isConnected()) {
				return true;
		} else if (fSessionLostHandler!=null) {
				Activator.trace("TelnetConnectorService.isConnected: false -> sessionLost"); //$NON-NLS-1$
				fSessionLostHandler.sessionLost();
		}
		
		return false;
	}

	/**
	 * @return false
	 * @see org.eclipse.rse.core.subsystems.AbstractConnectorService#requiresPassword()
	 */
	public boolean requiresPassword() {
		return false;
	}
	
	
	
}
