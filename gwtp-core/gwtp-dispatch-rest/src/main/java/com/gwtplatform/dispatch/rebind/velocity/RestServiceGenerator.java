/**
 * Copyright 2013 ArcBees Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gwtplatform.dispatch.rebind.velocity;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.PrintWriter;

public class RestServiceGenerator extends Generator {
    private static String SUFFIX = "Impl";

    private Logger logger;
    private TypeOracle typeOracle;
    private JClassType type;
    private String packageName;
    private String className;
    private Injector injector;
    private GeneratorFactory generatorFactory;

    @Override
    public String generate(TreeLogger treeLogger, GeneratorContext generatorContext,
            String typeName) throws UnableToCompleteException {
        logger = new Logger(treeLogger);
        typeOracle = generatorContext.getTypeOracle();
        type = getType(typeName);

        PrintWriter printWriter = tryCreatePrintWriter(generatorContext);
        if (printWriter == null) {
            return typeName + SUFFIX;
        }

        injector = Guice.createInjector(new RebindModule(logger, generatorContext));
        generatorFactory = injector.getInstance(GeneratorFactory.class);

        generateActions(type);

        ClassSourceFileComposerFactory composer = initComposer();
        SourceWriter sourceWriter = composer.createSourceWriter(generatorContext, printWriter);
        sourceWriter.commit(treeLogger);

        return typeName + SUFFIX;
    }

    private void generateActions(JClassType service) throws UnableToCompleteException {
        JMethod[] actionMethods = service.getMethods();
        if (actionMethods != null) {
            for (JMethod actionMethod : actionMethods) {
                generateRestAction(actionMethod);
            }
        }
    }

    private void generateRestAction(JMethod actionMethod) throws UnableToCompleteException {
        RestActionGenerator generator = generatorFactory.create(actionMethod);
        try {
            generator.generate();
        } catch (Exception e) {
            throw new UnableToCompleteException();
        }
//        try {
//            String actionClassName = new com.gwtplatform.dispatch.rebind.RestActionGenerator(baseRestPath)
//                    .generate(getLogger(), getGeneratorContext(), actionMethod);
//
//            actions.put(actionMethod, actionClassName);
//        } catch (UnableToCompleteException e) {
//            String readableDeclaration = actionMethod.getReadableDeclaration(true, true, true, true, true);
//            getLogger().log(TreeLogger.Type.ERROR, "Unable to generate rest action for method " +
//                    readableDeclaration + ".");
//
//            throw new UnableToCompleteException();
//        }
    }

    private JClassType getType(String typeName) throws UnableToCompleteException {
        try {
            return typeOracle.getType(typeName);
        } catch (NotFoundException e) {
            logger.die("Cannot find " + typeName);
        }

        return null;
    }

    private PrintWriter tryCreatePrintWriter(GeneratorContext generatorContext) throws UnableToCompleteException {
        packageName = type.getPackage().getName();
        className = type.getName() + SUFFIX;

        return generatorContext.tryCreate(logger.getTreeLogger(), packageName, className);
    }

    private ClassSourceFileComposerFactory initComposer() {
        ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, className);
        composer.addImport(type.getQualifiedSourceName());
        composer.addImplementedInterface(type.getName());

        return composer;
    }
}