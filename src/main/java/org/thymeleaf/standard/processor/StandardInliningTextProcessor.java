/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2014, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf.standard.processor;

import org.thymeleaf.context.ITemplateProcessingContext;
import org.thymeleaf.engine.ITextStructureHandler;
import org.thymeleaf.inline.ITextInliner;
import org.thymeleaf.inline.NoOpTextInliner;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.text.AbstractTextProcessor;
import org.thymeleaf.templatemode.TemplateMode;

/**
 *
 * @author Daniel Fern&aacute;ndez
 *
 * @since 3.0.0
 *
 */
public final class StandardInliningTextProcessor extends AbstractTextProcessor {

    public static final int PRECEDENCE = 1000;

    public StandardInliningTextProcessor(final TemplateMode templateMode) {
        super(templateMode, PRECEDENCE);
    }


    @Override
    protected void doProcess(final ITemplateProcessingContext processingContext, final IText text,
                        final ITextStructureHandler structureHandler) {


        final ITextInliner textInliner = processingContext.getVariables().getTextInliner();

        if (textInliner == null || textInliner == NoOpTextInliner.INSTANCE) {
            return;
        }

        // Execute the inliner directly passing the IText node (which is also a CharSequence)
        final CharSequence result = textInliner.inline(processingContext, text, text.isWhitespace());

        if (result == text) {
            // Either there were no changes, or they were directly done on the IText node itself. Either way,
            // nothing we should worry about
            return;
        }

        // Text was changed, so we need to set it as the new text
        // NOTE it is the responsibility of the inliner to correctly perform any HTML escaping operations
        text.setText(result.toString());

    }

}
