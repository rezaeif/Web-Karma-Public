/*******************************************************************************
 * Copyright 2012 University of Southern California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This code was developed by the Information Integration Group as part 
 * of the Karma project at the Information Sciences Institute of the 
 * University of Southern California.  For more information, publications, 
 * and related projects, please see: http://www.isi.edu/integration
 ******************************************************************************/
package edu.isi.karma.controller.command.importdata;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.command.CommandWithPreview;
import edu.isi.karma.controller.update.CSVImportPreviewUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.controller.update.WorksheetListUpdate;
import edu.isi.karma.imp.csv.CSVFileImport;
import edu.isi.karma.rep.Worksheet;
import edu.isi.karma.rep.Workspace;
import edu.isi.karma.view.VWorksheet;
import edu.isi.karma.view.VWorkspace;

public class ImportCSVFileCommand extends CommandWithPreview {

	private File csvFile;

	// Index of the column headers row
	private int headerRowIndex = 1;

	// Index of the row from where data starts
	private int dataStartRowIndex = 2;

	// Column delimiter
	private char delimiter = ',';

	// Quote character
	private char quoteCharacter = '"';

	// Escape character
	private char escapeCharacter = '\\';

	private VWorkspace vWorkspace;

	protected enum InteractionType {
		generatePreview, importTable
	}

	// Logger object
	private static Logger logger = LoggerFactory
			.getLogger(ImportCSVFileCommand.class.getSimpleName());

	public void setHeaderRowIndex(int headerRowIndex) {
		this.headerRowIndex = headerRowIndex;
	}

	public void setDataStartRowIndex(int dataStartRowIndex) {
		this.dataStartRowIndex = dataStartRowIndex;
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public void setQuoteCharacter(char escapeCharacter) {
		this.quoteCharacter = escapeCharacter;
	}

	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public ImportCSVFileCommand(String id, File file, VWorkspace vWorkspace) {
		super(id);
		this.csvFile = file;
		this.vWorkspace = vWorkspace;
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Import CSV File";
	}

	@Override
	public String getDescription() {
		if (isExecuted()) {
			return csvFile.getName();
		}
		return "";
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notUndoable;
	}

	@Override
	public UpdateContainer doIt(VWorkspace vWorkspace) throws CommandException {
		Workspace ws = vWorkspace.getWorkspace();
		CSVFileImport imp = new CSVFileImport(headerRowIndex,
				dataStartRowIndex, delimiter, quoteCharacter, csvFile,
				ws.getFactory(), ws);
		UpdateContainer c = new UpdateContainer();
		Worksheet wsht = null;
		try {
			wsht = imp.generateWorksheet();
			vWorkspace.addAllWorksheets();
			c.add(new WorksheetListUpdate(vWorkspace.getVWorksheetList()));
			VWorksheet vw = vWorkspace.getVWorksheet(wsht.getId());
			vw.update(c);
		} catch (Exception e) {
			logger.error("Error occured while importing CSV file.", e);
			return new UpdateContainer(new ErrorUpdate(
					"Error occured while importing CSV File."));
		}
		return c;
	}

	@Override
	public UpdateContainer undoIt(VWorkspace vWorkspace) {
		// Do nothing!
		return null;
	}

	@Override
	public UpdateContainer showPreview(VWorkspace vWorkspace)
			throws CommandException {
		UpdateContainer c = new UpdateContainer();
		c.add(new CSVImportPreviewUpdate(delimiter, quoteCharacter,
				escapeCharacter, csvFile, headerRowIndex, dataStartRowIndex, id));
		return c;
	}

	@Override
	public UpdateContainer handleUserActions(HttpServletRequest request) {
		/** Set the parameters **/
		// Set the delimiter
		if (request.getParameter("delimiter").equals("comma"))
			setDelimiter(',');
		else if (request.getParameter("delimiter").equals("tab"))
			setDelimiter('\t');
		else if (request.getParameter("delimiter").equals("space"))
			setDelimiter(' ');
		else {
			// TODO What to do with manual text delimiter
		}

		// Set the Header row index
		String headerIndex = request.getParameter("CSVHeaderLineIndex");
		if (headerIndex != "") {
			try {
				int index = Integer.parseInt(headerIndex);
				setHeaderRowIndex(index);
			} catch (Throwable t) {
				// TODO How t do handle illegal user inputs?
				logger.error("Wrong user input for CSV Header line index");
				return null;
			}
		} else
			setHeaderRowIndex(0);

		// Set the data start row index
		String dataIndex = request.getParameter("startRowIndex");
		if (dataIndex != "") {
			try {
				int index = Integer.parseInt(dataIndex);
				setDataStartRowIndex(index);
			} catch (Throwable t) {
				logger.error("Wrong user input for Data start line index");
				return null;
			}
		} else
			setDataStartRowIndex(2);

		/** Send response based on the interaction type **/
		UpdateContainer c = null;
		InteractionType type = InteractionType.valueOf(request
				.getParameter("interactionType"));
		switch (type) {
		case generatePreview: {
			try {
				c = showPreview(vWorkspace);
			} catch (CommandException e) {
				logger.error(
						"Error occured while creating utput JSON for CSV Import",
						e);
			}
			return c;
		}
		case importTable:
			return c;
		}
		return c;
	}
}
