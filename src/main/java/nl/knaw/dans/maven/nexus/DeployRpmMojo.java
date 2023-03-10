/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.maven.nexus;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "deploy-rpm", defaultPhase = LifecyclePhase.DEPLOY)
public class DeployRpmMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The number of RPMs that is expected to be built under target/rpm. Defaults to 1.
     */
    @Parameter(property = "expectedNumberOfRpms", defaultValue = "1", required = true)
    private int expectedNumberOfRpms;

    /**
     * Nexus username of the account with which to deploy the RPM.
     */
    @Parameter(property = "nexusUserName", required = true)
    private String nexusUserName;

    /**
     * Nexus password of the account with which to deploy the RPM.
     */
    @Parameter(property = "nexusPassword", required = true)
    private String nexusPassword;

    /**
     * URL of the YUM repository for snapshot releases
     */
    @Parameter(property = "snapshotRpmRepositoryUrl")
    private String snapshotRpmRepositoryUrl;

    /**
     * URL of the YUM repository for final releases
     */
    @Parameter(property = "rpmRepositoryUrl", required = true)
    private String rpmRepositoryUrl;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Path> rpms = getRpmFiles();
        if (expectedNumberOfRpms != rpms.size()) {
            throw new MojoExecutionException(String.format("Expected %d RPMs and found %d", expectedNumberOfRpms, rpms.size()));
        }
        getLog().info(String.format("Found the following RPM(s): %s", rpms));

        for (Path rpm : rpms) {
            HttpURLConnection conn = openConnection(getRpmRepoUrl(rpm.getFileName().toString()));

            try {
                DeployResponse r = putFile(conn, rpm);
                if (r.getCode() != 200) {
                    getLog().error(String.format("Could not deploy %s, Nexus returned: %s", rpm, r.getStatusLine()));
                }
            }
            finally {
                conn.disconnect();
            }
        }
    }

    private URL getRpmRepoUrl(String rpmBaseName) throws MojoFailureException {
        String uploadUrl = appendSlash(getRpmRepositoryUrl()) + rpmBaseName;
        getLog().info(String.format("%s -> %s", rpmBaseName, uploadUrl));
        try {
            return new URL(uploadUrl);
        }
        catch (MalformedURLException e) {
            throw new MojoFailureException(String.format("Artifactor repository URL malformed: %s", uploadUrl), e);
        }
    }

    private String getRpmRepositoryUrl() {
        boolean isSnapshot = project.getVersion().contains("SNAPSHOT");
        if (isSnapshot && snapshotRpmRepositoryUrl != null) {
            return snapshotRpmRepositoryUrl;
        }
        return rpmRepositoryUrl;
    }

    private String appendSlash(String s) {
        return s.endsWith("/") ? s : s + "/";
    }

    private HttpURLConnection openConnection(URL url) throws MojoExecutionException {
        try {
            return (HttpURLConnection) url.openConnection();
        }
        catch (IOException e) {
            throw new MojoExecutionException(String.format("Cannot open connection to %s", url), e);
        }
    }

    private DeployResponse putFile(HttpURLConnection conn, Path file) throws MojoExecutionException {
        try {
            String auth = nexusUserName + ":" + nexusPassword;
            byte[] encodedAuth = java.util.Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            conn.setRequestProperty("Authorization", authHeaderValue);
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            getLog().debug(String.format("Start uploading %s to %s", file, conn.getURL()));
            FileUtils.copyFile(file.toFile(), conn.getOutputStream());
            return new DeployResponse(conn.getResponseCode(), conn.getResponseMessage());
        }
        catch (IOException e) {
            throw new MojoExecutionException(String.format("Could not PUT file %s to %s", file, conn.getURL()), e);
        }
    }

    private List<Path> getRpmFiles() throws MojoExecutionException, MojoFailureException {
        Path target = Paths.get(project.getBuild().getDirectory());
        Path rpm = target.resolve("rpm");
        if (Files.isDirectory(rpm)) {
            try (Stream<Path> walk = Files.walk(rpm)) {
                return walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".rpm"))
                    .collect(Collectors.toList());
            }
            catch (IOException e) {
                throw new MojoFailureException("Error searching for RPM files", e);
            }
        }
        else {
            throw new MojoExecutionException("No directory found at target/rpm");
        }
    }

}

