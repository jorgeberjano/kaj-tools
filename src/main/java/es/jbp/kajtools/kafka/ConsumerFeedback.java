package es.jbp.kajtools.kafka;

import es.jbp.kajtools.kafka.RecordItem;
import java.util.List;

public interface ConsumerFeedback {

    void consumedRecords(List<RecordItem> records);

    void message(String message);

    void finished();

}
