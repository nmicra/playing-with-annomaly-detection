package com.github.nmicra.ipannomalitydetection

import org.springframework.stereotype.Service
import org.tribuo.Example
import org.tribuo.Feature
import org.tribuo.MutableDataset
import org.tribuo.anomaly.AnomalyFactory
import org.tribuo.anomaly.Event
import org.tribuo.anomaly.evaluation.AnomalyEvaluator
import org.tribuo.anomaly.libsvm.LibSVMAnomalyModel
import org.tribuo.anomaly.libsvm.LibSVMAnomalyTrainer
import org.tribuo.anomaly.libsvm.SVMAnomalyType
import org.tribuo.common.libsvm.KernelType
import org.tribuo.common.libsvm.LibSVMModel
import org.tribuo.common.libsvm.SVMParameters
import org.tribuo.data.columnar.FieldProcessor
import org.tribuo.data.columnar.RowProcessor
import org.tribuo.data.columnar.processors.field.DoubleFieldProcessor
import org.tribuo.data.columnar.processors.response.FieldResponseProcessor
import org.tribuo.datasource.ListDataSource
import org.tribuo.impl.ArrayExample
import org.tribuo.provenance.SimpleDataSourceProvenance
import kotlin.system.measureTimeMillis


@Service
class AnomalityService {
    private val MODEL_GAMMA = 1.0

    // One class SVM parameter: 0.1 indicates that at most 10% of the training examples are allowed to be wrongly
    // classified. And at least 10% of the training examples will act as support vectors.
    private val MODEL_NU = 0.1

    private val params = SVMParameters(SVMAnomalyType(SVMAnomalyType.SVMMode.ONE_CLASS), KernelType.RBF).apply {
        gamma = MODEL_GAMMA
        setNu(MODEL_NU)
    }
    private val trainer = LibSVMAnomalyTrainer(params)

    private val anomalyFactory =  AnomalyFactory()

    lateinit var model : LibSVMModel<Event>

    private val fieldProcessors = mapOf<String, FieldProcessor>("myrange" to DoubleFieldProcessor("myrange"))
    private val responseProcessor = FieldResponseProcessor("status", "ANOMALOUS_EVENT", anomalyFactory)

    private val sRowProcessor : RowProcessor<Event> =
        RowProcessor<Event>(responseProcessor, fieldProcessors)




    fun train(){

        val trainingDataExamples: MutableList<Example<Event>> = mutableListOf()
        val testingDataExamples: MutableList<Example<Event>> = mutableListOf()
        repeat(10000) {// main training data
            val num = (10000..99000).random()
            trainingDataExamples.add(ArrayExample(AnomalyFactory.EXPECTED_EVENT,listOf(Feature("myrange", num.toDouble()))))
        }

        repeat(500) {// testing data positive
            val num = (10000..99000).random()
            testingDataExamples.add(ArrayExample(AnomalyFactory.EXPECTED_EVENT,listOf(Feature("myrange", num.toDouble()))))
        }
        repeat(100) {// testing data negative
            val num = (1..9900).random()
            testingDataExamples.add(ArrayExample(AnomalyFactory.ANOMALOUS_EVENT,listOf(Feature("myrange", num.toDouble()))))
        }

        repeat(100) {// testing data negative
            val num = (100000..1000000).random()
            testingDataExamples.add(ArrayExample(AnomalyFactory.ANOMALOUS_EVENT,listOf(Feature("myrange", num.toDouble()))))
        }


        val trainingProvenance = SimpleDataSourceProvenance("Anomaly training data", anomalyFactory)
        val trainData = MutableDataset(ListDataSource(trainingDataExamples, anomalyFactory, trainingProvenance))

        val testingProvenance = SimpleDataSourceProvenance("Anomaly testing data", anomalyFactory)
        val testingData = MutableDataset(ListDataSource(testingDataExamples, anomalyFactory, testingProvenance))

        val elapsedTime = measureTimeMillis {
            model = trainer.train(trainData);
        }

        println(">>>> Training took $elapsedTime millis")
        println(">>>> number of supported vectors: ${(model as LibSVMAnomalyModel).numberOfSupportVectors}")

        var testEvaluation = AnomalyEvaluator().evaluate(model,testingData);
        println(testEvaluation.toString());
        println(testEvaluation.confusionString());
    }

 /*   fun queryModel(ip : String) : String {
        val ipLongVal = ip.replace(".","").toLong()
        val queryRow = mapOf("ipvalue" to "$ipLongVal")

        val row: ColumnarIterator.Row = ColumnarIterator.Row(0, queryRow.keys.toList(), queryRow)

        val exmpl = sRowProcessor.generateExample(row,false).get()
        val prediction = model.predict(exmpl)
        println(">>> example => $exmpl ")
        return ">>> prediction => ${prediction.output}, score= ${prediction.outputScores}, type= ${prediction.output.type}"
    }*/

    fun queryModel(num : Int) : String {

        val exmpl = ArrayExample(AnomalyFactory.EXPECTED_EVENT,listOf(Feature("myrange", num.toDouble())))
        val prediction = model.predict(exmpl)
        println(">>> example => $exmpl ")
        println (">>> prediction => ${prediction.output}, score= ${prediction.outputScores}, type= ${prediction.output.type}")
        return prediction.output.type.toString()
    }
}