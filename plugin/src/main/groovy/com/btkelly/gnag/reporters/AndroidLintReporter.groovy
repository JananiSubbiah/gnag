/*
 * Copyright 2016 Bryan Kelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.btkelly.gnag.reporters

import com.btkelly.gnag.extensions.AndroidLintExtension
import com.btkelly.gnag.utils.GnagReportBuilder
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project

import static com.btkelly.gnag.extensions.AndroidLintExtension.SEVERITY_ERROR
import static com.btkelly.gnag.extensions.AndroidLintExtension.SEVERITY_WARNING

/**
 * Created by bobbake4 on 4/18/16.
 */
class AndroidLintReporter implements Reporter {

    private final Project project
    private final AndroidLintExtension androidLintExtension;

    AndroidLintReporter(AndroidLintExtension androidLintExtension, Project project) {
        this.project = project
        this.androidLintExtension = androidLintExtension
    }

    @Override
    boolean isEnabled() {

        if (androidLintExtension.enabled) {
            if (reportFile().exists()) {
                return true
            } else {
                println "Android Lint Reporter is enabled but no lint report was found"
                return false
            }
        } else {
            return false
        }
    }

    @Override
    boolean hasErrors() {
        GPathResult xml = new XmlSlurper().parseText(reportFile().text)
        int numErrors = xml.issue.count { severityEnabled(it.@severity.text()) }
        println "Android Lint report executed, found " + numErrors + " errors."
        return numErrors != 0
    }

    @Override
    String reporterName() {
        return "Android Lint"
    }

    @Override
    File reportFile() {
        return new File(new FileNameByRegexFinder().getFileNames(project.buildDir.path + "/outputs/", "lint-results.+\\.xml").first())
    }

    @Override
    void appendReport(GnagReportBuilder gnagReportBuilder) {

        gnagReportBuilder.insertReporterHeader(reporterName())

        GPathResult xml = new XmlSlurper().parseText(reportFile().text)

        xml.issue.findAll { severityEnabled(it.@severity.text()) }.each { violation ->
            gnagReportBuilder.appendViolation(
                    violation.@id.text(),
                    violation.@url.text(),
                    violation.location.@file.text(),
                    violation.location.@line.text(),
                    violation.@message.text()
            )
        }
    }

    private boolean severityEnabled(String severity) {
        if (androidLintExtension.severity.equals(SEVERITY_WARNING)) {
            return true
        } else {
            return severity.equals(SEVERITY_ERROR)
        }
    }
}
