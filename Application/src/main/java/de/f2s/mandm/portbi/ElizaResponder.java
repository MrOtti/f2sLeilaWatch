/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.f2s.mandm.portbi;

import java.util.HashMap;

/**
 * A basic chat bot based on ELIZA, one of the earliest programs to employ primitive natural
 * language processing. Like ELIZA, this bot can simulate a Rogerian psychotherapist using a
 * collection of vague and generic responses.
 */
public class ElizaResponder {

    private static HashMap<String, String> CONVERSATION = new HashMap();

    private String previousInput = null;

    public void populateConversation() {
        CONVERSATION.put("greeting", "Hello, Leila! You have no pending notifications.");
        CONVERSATION.put("in_notification", "Do you want to place this order?");
        CONVERSATION.put("not_order", "Would you prefer a reminder tomorrow during work hours?");
        CONVERSATION.put("order", "Done! Should I send a notification?");
        CONVERSATION.put("out_notification", "Great. Have a nice day!");
        CONVERSATION.put("reminder", "I'll take care of that. Have a nice day!");
    }

    public ElizaResponder() {
        populateConversation();
    }

    public String elzTalk(String input) {
        if (null == input) {
            input = "";
        }
        String result = "";

        input = " " + input.toLowerCase().replace("\'", "") + " ";

        if (previousInput != null && input.equals(previousInput)) {
            return "Didn't you just say that?\n";
        }
        previousInput = input;

        int keywordIndex = 0;
        /*for (; keywordIndex < CONVERSATION_KEYWORDS.length; ++keywordIndex) {
            int index = input.indexOf(CONVERSATION_KEYWORDS[keywordIndex]);
            if (index != -1) {
                break;
            }
        }

        String afterKeyword = "";
        if (keywordIndex == CONVERSATION_KEYWORDS.length) {
            keywordIndex = 0;
        } else {
            int index = input.indexOf(CONVERSATION_KEYWORDS[keywordIndex]);
            afterKeyword = input.substring(index + CONVERSATION_KEYWORDS[keywordIndex].length());
            String[] parts = afterKeyword.split("\\s+");
            for (int i = 0; i < WORDS_TO_REPLACE.length / 2; i++) {
                String first = WORDS_TO_REPLACE[i * 2];
                String second = WORDS_TO_REPLACE[i * 2 + 1];
                for (int j = 0; j < parts.length; ++j) {
                    if (parts[j].equals(first)) {
                        parts[j] = second;
                    } else if (parts[j].equals(second)) {
                        parts[j] = first;
                    }
                }
            }
            afterKeyword = TextUtils.join(" ", parts);
        }

        String question = QUESTIONS[responseCurrentIndices[keywordIndex] - 1];
        responseCurrentIndices[keywordIndex] = responseCurrentIndices[keywordIndex] + 1;
        if (responseCurrentIndices[keywordIndex] > responseEnds[keywordIndex]) {
            responseCurrentIndices[keywordIndex] = responseStarts[keywordIndex];
        }
        result += question;
        if (result.endsWith("*")) {
            result = result.substring(0, result.length() - 1);
            result += " " + afterKeyword;
        }

*/
        String transformedInput = processInput(input);
        String response = CONVERSATION.get(transformedInput);

        result += response;
        return result;
    }

    private String processInput(String input) {
        return "greeting";
    }
}
