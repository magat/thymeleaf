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
package org.thymeleaf.templateparser.markup;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.Reader;
import java.io.StringReader;

import org.attoparser.IMarkupHandler;
import org.attoparser.IMarkupParser;
import org.attoparser.MarkupParser;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.select.BlockSelectorMarkupHandler;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.engine.ITemplateHandler;
import org.thymeleaf.engine.TemplateHandlerAdapterMarkupHandler;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.resource.CharArrayResource;
import org.thymeleaf.resource.IResource;
import org.thymeleaf.resource.ReaderResource;
import org.thymeleaf.resource.StringResource;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateparser.ITemplateParser;
import org.thymeleaf.templateparser.reader.ParserLevelCommentMarkupReader;
import org.thymeleaf.templateparser.reader.PrototypeOnlyCommentMarkupReader;
import org.thymeleaf.util.Validate;

/**
 *
 * @author Daniel Fern&aacute;ndez
 * @since 3.0.0
 * 
 */
public abstract class AbstractMarkupTemplateParser implements ITemplateParser {


    private final IMarkupParser parser;
    private final boolean html;



    protected AbstractMarkupTemplateParser(final ParseConfiguration parseConfiguration, final int bufferPoolSize, final int bufferSize) {
        super();
        Validate.notNull(parseConfiguration, "Parse configuration cannot be null");
        this.parser = new MarkupParser(parseConfiguration, bufferPoolSize, bufferSize);
        this.html = parseConfiguration.getMode().equals(ParseConfiguration.ParsingMode.HTML);
    }




    /*
     * -------------------
     * PARSE METHODS
     * -------------------
     */



    public final void parseTemplate(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final String[] selectors,
            final ITemplateHandler templateHandler) {
        parse(configuration, templateMode, templateResource, true, selectors, templateHandler);
    }


    public final void parseFragment(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final String[] selectors,
            final ITemplateHandler templateHandler) {
        parse(configuration, templateMode, templateResource, false, selectors, templateHandler);
    }



    private void parse(
            final IEngineConfiguration configuration,
            final TemplateMode templateMode,
            final IResource templateResource,
            final boolean topLevel,
            final String[] selectors,
            final ITemplateHandler templateHandler) {

        Validate.notNull(configuration, "Engine Configuration cannot be null");
        Validate.notNull(templateMode, "Template Mode cannot be null");
        Validate.notNull(templateResource, "Template Resource cannot be null");
        // Selectors CAN be null
        Validate.notNull(templateHandler, "Template Handler cannot be null");

        if (templateMode.isHTML()) {
            Validate.isTrue(this.html, "Parser is configured as XML, but HTML-mode template parsing is being requested");
        } else if (templateMode.isXML()) {
            Validate.isTrue(!this.html, "Parser is configured as HTML, but XML-mode template parsing is being requested");
        } else {
            throw new IllegalArgumentException(
                    "Parser is configured as " + (this.html? "HTML" : "XML") + " but an unsupported template mode " +
                    "has been specified: " + templateMode);
        }

        final String templateResourceName = templateResource.getName();

        try {

            // The final step of the handler chain will be the adapter that will convert attoparser's handler chain to thymeleaf's.
            IMarkupHandler handler =
                        new TemplateHandlerAdapterMarkupHandler(
                                templateResourceName,
                                topLevel,
                                templateHandler,
                                configuration.getTextRepository(),
                                configuration.getElementDefinitions(),
                                configuration.getAttributeDefinitions(),
                                templateMode);

            // If we need to select blocks, we will need a block selector here. Note this will get executed in the
            // handler chain AFTER thymeleaf's own TemplateHandlerAdapterMarkupHandler, so that we will be able to
            // include in selectors code inside prototype-only comments.
            if (selectors != null) {

                final String standardDialectPrefix = configuration.getStandardDialectPrefix();

                final TemplateFragmentMarkupReferenceResolver referenceResolver =
                        (standardDialectPrefix != null ?
                            TemplateFragmentMarkupReferenceResolver.forPrefix(this.html, standardDialectPrefix) : null);
                handler = new BlockSelectorMarkupHandler(handler, selectors, referenceResolver);
            }

            // Each type of resource will require a different parser method to be called.
            final Reader templateReader;
            if (templateResource instanceof ReaderResource) {

                final Reader resourceReader = ((ReaderResource)templateResource).getContent();
                templateReader = new ParserLevelCommentMarkupReader(new PrototypeOnlyCommentMarkupReader(new BufferedReader(resourceReader)));

            } else if (templateResource instanceof StringResource) {

                final Reader resourceReader = new StringReader(((StringResource)templateResource).getContent());
                templateReader = new ParserLevelCommentMarkupReader(new PrototypeOnlyCommentMarkupReader(resourceReader));

            } else if (templateResource instanceof CharArrayResource) {

                final CharArrayResource charArrayResource = (CharArrayResource) templateResource;
                final CharArrayReader charArrayReader =
                        new CharArrayReader(charArrayResource.getContent(), charArrayResource.getOffset(), charArrayResource.getLen());
                templateReader = new ParserLevelCommentMarkupReader(new PrototypeOnlyCommentMarkupReader(charArrayReader));

            } else {

                throw new IllegalArgumentException(
                        "Cannot parse: unrecognized " + IResource.class.getSimpleName() + " implementation: " + templateResource.getClass().getName());

            }

            this.parser.parse(templateReader, handler);


        } catch (final ParseException e) {
            final String message = "An error happened during template parsing";
            if (e.getLine() != null && e.getCol() != null) {
                throw new TemplateInputException(message, templateResource.getName(), e.getLine().intValue(), e.getCol().intValue(), e);
            }
            throw new TemplateInputException(message, templateResource.getName(), e);
        }

    }

    
    
}
