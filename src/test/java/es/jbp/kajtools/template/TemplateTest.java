package es.jbp.kajtools.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import es.jbp.kajtools.util.ResourceUtil;
import java.util.Collections;
import org.junit.Test;

public class TemplateTest {

    @Test
    public void test() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template.json");
        String expected = ResourceUtil.readResourceString("expected.json");

        String actual = TextTemplate.builder()
            .variables(Collections.singletonMap("variable", "valor"))
            .build()
            .process(template);

        assertEquals(expected, actual);
    }

    @Test
    public void testUUID() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template2.json");
        String actual = TextTemplate.builder()
            .variables(Collections.singletonMap("variable", "valor"))
            .build()
            .process(template);

        assertNotNull(actual);

    }
}
