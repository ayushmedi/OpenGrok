package org.opensolaris.opengrok.analysis.clojure;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensolaris.opengrok.analysis.Ctags;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.Scopes;
import org.opensolaris.opengrok.analysis.StreamSource;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.search.QueryBuilder;
import org.opensolaris.opengrok.util.TestRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.opensolaris.opengrok.analysis.AnalyzerGuru.string_ft_nstored_nanalyzed_norms;

/**
 * @author Farid Zakaria
 */
public class ClojureAnalyzerFactoryTest {

    FileAnalyzer analyzer;
    private final String ctagsProperty = "org.opensolaris.opengrok.analysis.Ctags";
    private static Ctags ctags;
    private static TestRepository repository;

    public ClojureAnalyzerFactoryTest() {
        ClojureAnalyzerFactory analFact = new ClojureAnalyzerFactory();
        this.analyzer = analFact.getAnalyzer();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setCtags(System.getProperty(ctagsProperty, "ctags"));
        if (env.validateExuberantCtags()) {
            this.analyzer.setCtags(new Ctags());
        }
    }

    private static StreamSource getStreamSource(final String fname) {
        return new StreamSource() {
            @Override
            public InputStream getStream() throws IOException {
                return new FileInputStream(fname);
            }
        };
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ctags = new Ctags();
        ctags.setBinary(RuntimeEnvironment.getInstance().getCtags());

        repository = new TestRepository();
        repository.create(ClojureAnalyzerFactoryTest.class.getResourceAsStream(
                "/org/opensolaris/opengrok/index/source.zip"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ctags.close();
        ctags = null;
    }

    /**
     * Test of writeXref method, of class CAnalyzerFactory.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testScopeAnalyzer() throws Exception {
        String path = repository.getSourceRoot() + "/clojure/sample.clj";
        File f = new File(path);
        if (!(f.canRead() && f.isFile())) {
            fail("clojure testfile " + f + " not found");
        }

        Document doc = new Document();
        doc.add(new Field(QueryBuilder.FULLPATH, path,
                          string_ft_nstored_nanalyzed_norms));
        StringWriter xrefOut = new StringWriter();
        analyzer.setCtags(ctags);
        analyzer.analyze(doc, getStreamSource(path), xrefOut);

        Definitions definitions = Definitions.deserialize(doc.getField(QueryBuilder.TAGS).binaryValue().bytes);

        String[] type = new String[1];
        assertTrue(definitions.hasDefinitionAt("opengrok", 4, type));
        assertThat(type[0], is("namespace"));
        assertTrue(definitions.hasDefinitionAt("power-set", 8, type));
        assertThat(type[0], is("function"));
        assertTrue(definitions.hasDefinitionAt("power-set-private", 14, type));
        assertThat(type[0], is("private function"));
        assertTrue(definitions.hasDefinitionAt("author", 19, type));
        assertThat(type[0], is("struct"));
        assertTrue(definitions.hasDefinitionAt("author-first-name", 22, type));
        assertThat(type[0], is("definition"));
        assertTrue(definitions.hasDefinitionAt("Farid", 24, type));
        assertThat(type[0], is("definition"));
    }


}
