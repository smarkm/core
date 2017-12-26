package com.dotcms.rest.api.v1.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonParser;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationContext;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonDeserializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ContentTypeResource}'s form
 */
@JsonDeserialize(using = ContentTypeForm.ContentTypeFormDeserialize.class)
public class ContentTypeForm  {

    private List<ContentTypeFormEntry> entries;

    public ContentTypeForm(List<ContentTypeFormEntry> entries) {
        this.entries = entries;
    }

    public Iterable<ContentTypeFormEntry> getIterable() {
        return entries;
    }

    public ContentType getContentType() {
        return entries.get(0).contentType;
    }

    public List<String> getWorkflowsIds() {
        return entries.get(0).workflowsIds;
    }

    public static final class ContentTypeFormDeserialize extends JsonDeserializer<ContentTypeForm> {

        public static final String WORKFLOW_ATTRIBUTE_NAME = "workflow";

        @Override
        public ContentTypeForm deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {

            String json = jsonParser.readValueAsTree().toString();
            return buildForm(json);
        }

        @VisibleForTesting
        public ContentTypeForm buildForm(String json) {
            final List<ContentType> typesToSave = new JsonContentTypeTransformer(json).asList();
            final List<List<String>> workflows = getWorkflowIdsFromJson(json);

            List<ContentTypeFormEntry> entries = getContentTypeFormEntries(typesToSave, workflows);

            return new ContentTypeForm(entries);
        }

        private List<ContentTypeFormEntry> getContentTypeFormEntries(List<ContentType> typesToSave,
                                                                     List<List<String>> workflows) {

            List<ContentTypeFormEntry> entries = new ArrayList<>();

            for (int i = 0; i < workflows.size(); i++) {
                List<String> worflows = workflows.get(i);
                ContentType contentType = typesToSave.get(i);

                ContentTypeFormEntry entry = new ContentTypeFormEntry(contentType, worflows);
                entries.add(entry);
            }
            return entries;
        }

        private static List<List<String>> getWorkflowIdsFromJson(String json) {
            final List<List<String>> workflows = new ArrayList<>();

            try {
                JSONArray jarr = new JSONArray(json);

                for (int i = 0; i < jarr.size(); i++) {
                    final JSONObject fieldJsonObject = (JSONObject) jarr.get(i);
                    List<String> workflowsIds = getWorkflowsId(fieldJsonObject);
                    workflows.add(workflowsIds);
                }
            } catch (JSONException e) {

                try {
                    final JSONObject  fieldJsonObject = new JSONObject(json);
                    List<String> workflowsIds = getWorkflowsId(fieldJsonObject);
                    workflows.add(workflowsIds);
                } catch (JSONException e1) {
                    throw new DotRuntimeException(e1);
                }
            }
            return workflows;
        }

        private static List<String> getWorkflowsId(JSONObject fieldJsonObject) throws JSONException {
            List<String> worflowsArray = new ArrayList<>();

            if (fieldJsonObject.has(WORKFLOW_ATTRIBUTE_NAME)) {
                JSONArray workflowsJaonArray = (JSONArray) fieldJsonObject.get(WORKFLOW_ATTRIBUTE_NAME);

                for (int k = 0; k < workflowsJaonArray.size(); k++) {
                    worflowsArray.add((String) workflowsJaonArray.get(k));
                }
            }

            return worflowsArray;
        }
    }

    public static class ContentTypeFormEntry {
        ContentType contentType;
        List<String> workflowsIds;

        public ContentTypeFormEntry(ContentType contentType, List<String> workflowsIds) {
            this.contentType = contentType;
            this.workflowsIds = workflowsIds;
        }
    }
}
