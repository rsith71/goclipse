/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package melnorme.lang.ide.ui.editor;


import static melnorme.utilbox.core.Assert.AssertNamespace.assertNotNull;
import static melnorme.utilbox.core.CoreUtil.tryCast;
import melnorme.utilbox.collections.ArrayList2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.texteditor.ITextEditor;

public class AbstractLangEditorActionContributor extends TextEditorActionContributor {
	
	public static interface IActiveEditorListener {
		
		public abstract void setActiveEditor(IEditorPart part);
		
	}
	
	protected final ArrayList2<IActiveEditorListener> activeEditorListeners = new ArrayList2<>();
	
	public AbstractLangEditorActionContributor() {
	}
	
	@Override
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
	}
	
	protected CommandContributionItem createEditorContribution(String commandId, String editorActionId) {
		final EditorActionHelper editorActionHelper = new EditorActionHelper(commandId, editorActionId);
		registerContribution(editorActionHelper);
		return editorActionHelper.createContributionItem(this);
	}
	
	protected <T extends IActiveEditorListener> T registerContribution(T editorContribution) {
		activeEditorListeners.add(editorContribution);
		return editorContribution;
	}
	
	protected CommandContributionItem createCommandContribution(String commandId) {
		IWorkbenchWindow svcLocator = getPage().getWorkbenchWindow();
		return new CommandContributionItem(
			new CommandContributionItemParameter(svcLocator, null, commandId, CommandContributionItem.STYLE_PUSH));
	}
	
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		
		for (IActiveEditorListener activeEditorListeners : activeEditorListeners) {
			activeEditorListeners.setActiveEditor(part);
		}
	}
	
	/* -----------------  ----------------- */
	
	/**
	 * Helper to contribute programmatically menu contributions based on an editor {@link Action} as a handler.
	 */
	public static class EditorActionHelper implements IActiveEditorListener {

		protected final String commandId;
		protected final String editorActionId;
		protected IHandlerActivation handlerActivation;

		public EditorActionHelper(String commandId, String editorActionId) {
			this.commandId = assertNotNull(commandId);
			this.editorActionId = assertNotNull(editorActionId);
		}
		
		public String getCommandId() {
			return commandId;
		}
		
		public CommandContributionItem createContributionItem(AbstractLangEditorActionContributor actionContributor) {
			return actionContributor.createCommandContribution(commandId);
		}
		
		@Override
		public void setActiveEditor(IEditorPart part) {
			ITextEditor textEditor = tryCast(part, ITextEditor.class);
			
			IHandlerService handlerService = getHandlerService(textEditor);
			
			if(handlerActivation != null) {
				handlerService.deactivateHandler(handlerActivation);
				handlerActivation = null;
			}
			IAction action = getAction(textEditor, editorActionId);
			if(action != null) {
				handlerActivation = handlerService.activateHandler(commandId, new ActionHandler(action));
			}
		}
		
		protected IAction getAction(ITextEditor editor, String actionId) {
			return (editor == null || actionId == null ? null : editor.getAction(actionId));
		}
		
	}
	
	protected static IHandlerService getHandlerService(ITextEditor textEditor) {
		return (IHandlerService) textEditor.getEditorSite().getService(IHandlerService.class);
	}
	
	/* -----------------  ----------------- */
	
	/**
	 * Helper to contribute programmatically menu contributions that use an {@link IEditorActionDelegate}
	 */
	public class ReusableAction extends Action implements IActiveEditorListener {
		
		protected final IEditorActionDelegate actionDelegate;
		
		public ReusableAction(String actionDefinitionid, String text, IEditorActionDelegate ad) {
			super(text);
			this.actionDelegate = ad;
			setActionDefinitionId(actionDefinitionid);
		}
		
		@Override
		public void setActiveEditor(IEditorPart targetEditor) {
			actionDelegate.setActiveEditor(this, targetEditor);
		}
		
		@Override
		public void run() {
			actionDelegate.run(this);
		}
	}
	
}