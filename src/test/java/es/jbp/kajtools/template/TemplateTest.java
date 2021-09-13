package es.jbp.kajtools.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import es.jbp.kajtools.util.TemplateExecutor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
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

    @Test
    public void testFormat() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template2.json");

        String encoded = textTemplate.encodeBeforeFormatting(template);
        String actual = textTemplate.decodeAfterFormatting(encoded);

        assertEquals(template, actual);
    }

    @Test
    public void testMemory() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template_memory.json");

        String actual = textTemplate.process(template);

        Map<String, Object> map = JsonUtils.toMap(actual);
        assertEquals(map.get("identifier"), map.get("identifier_bis"));
        assertEquals(map.get("random"), map.get("random_bis"));
        assertEquals(map.get("country"), map.get("country_bis"));
    }

    @Test
    public void testCounters() throws ExpressionException {
        String template = ResourceUtil.readResourceString("template_counters.json");

        TemplateExecutor templateExecutor = new TemplateExecutor();

        for (long i = 0; i < 10; i++) {
            String actual = templateExecutor.processTemplate(template);
            Map<String, Object> map = JsonUtils.toMap(actual);
            assertEquals(BigInteger.valueOf(i), new BigDecimal(Objects.toString(map.get("index"))).toBigInteger());
            assertEquals(BigInteger.valueOf(i), new BigDecimal(Objects.toString(map.get("counter"))).toBigInteger());
            templateExecutor.avanceCounters();
        }

        templateExecutor.resetIndexCounter();

        for (long i = 0; i < 10; i++) {
            String actual = templateExecutor.processTemplate(template);
            Map<String, Object> map = JsonUtils.toMap(actual);
            assertEquals(BigInteger.valueOf(i), new BigDecimal(Objects.toString(map.get("index"))).toBigInteger());
            assertEquals(BigInteger.valueOf(i + 10),
                new BigDecimal(Objects.toString(map.get("counter"))).toBigInteger());
            templateExecutor.avanceCounters();
        }


    }
}
