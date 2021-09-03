package es.jbp.kajtools.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import es.jbp.kajtools.util.ResourceUtil;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class TemplateTest {

    TextTemplate textTemplate = new TextTemplate();

    @Test
    public void test() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template.json");
        String expected = ResourceUtil.readResourceString("expected.json");

        textTemplate.setVariableValues(Collections.singletonMap("variable", "valor"));

        String actual = textTemplate.process(template);
        assertEquals(expected, actual);
    }

    @Test
    public void testUUID() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template2.json");
        textTemplate.setVariableValues(Collections.singletonMap("variable", "valor"));

        String actual = textTemplate.process(template);
        assertNotNull(actual);
    }
}
