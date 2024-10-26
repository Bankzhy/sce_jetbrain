package net.codeoasis.sce_jetbrain.listeners;/* ==========================================================
File:        CustomDocumentListener.java
Description: Logs time from document change events.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/



import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.codeoasis.sce_jetbrain.OasisActivator;
import net.codeoasis.sce_jetbrain.models.LineStats;

public class CustomDocumentListener implements BulkAwareDocumentListener.Simple {
    @Override
    public void documentChangedNonBulk(DocumentEvent documentEvent) {
        // WakaTime.log.debug("documentChangedNonBulk event");
        try {
            if (!OasisActivator.isAppActive()) return;
            Document document = documentEvent.getDocument();
            VirtualFile file = OasisActivator.getFile(document);
            if (file == null) return;
            Project project = OasisActivator.getProject(document);
            if (!OasisActivator.isProjectInitialized(project)) return;
            LineStats lineStats = OasisActivator.getLineStats(document);
            OasisActivator.appendHeartbeat(file, project, false, lineStats);
        } catch(Exception e) {
            OasisActivator.debugException(e);
        }
    }


}