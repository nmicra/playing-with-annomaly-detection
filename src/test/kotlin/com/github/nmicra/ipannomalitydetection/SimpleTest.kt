package com.github.nmicra.ipannomalitydetection

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
import org.tribuo.common.libsvm.SVMParameters
import org.tribuo.datasource.ListDataSource
import org.tribuo.impl.ArrayExample
import org.tribuo.provenance.SimpleDataSourceProvenance
import org.tribuo.util.Util


@SpringBootTest
class SimpleTest {

	@Autowired
	lateinit var anomalityService : AnomalityService

	@Test
	fun simpletest() {
		println(">>> starting =================================================================")
		anomalityService.train()

		println(">>> querying =================================================================")
		(11000..99000 step 1000).forEach {
			val result = anomalityService.queryModel(it)
			println("result is $result")
		}

	}

	@Test
	fun planC(){
		val trainingDataExamples: MutableList<Example<Event>> = mutableListOf()
		val testingDataExamples: MutableList<Example<Event>> = mutableListOf()
		repeat(10000) {
			val num = (10000..99000).random()
			trainingDataExamples.add(ArrayExample(AnomalyFactory.EXPECTED_EVENT,listOf(Feature("myrange", num.toDouble()))))
		}


		repeat(500) {
			val num = (10000..99000).random()
			testingDataExamples.add(ArrayExample(AnomalyFactory.EXPECTED_EVENT,listOf(Feature("myrange", num.toDouble()))))
		}
		repeat(100) {
			val num = (1..9900).random()
			testingDataExamples.add(ArrayExample(AnomalyFactory.ANOMALOUS_EVENT,listOf(Feature("myrange", num.toDouble()))))
		}

		repeat(100) {
			val num = (100000..1000000).random()
			testingDataExamples.add(ArrayExample(AnomalyFactory.ANOMALOUS_EVENT,listOf(Feature("myrange", num.toDouble()))))
		}

		val anomalyFactory =  AnomalyFactory()
		val trainingProvenance = SimpleDataSourceProvenance("Anomaly training data", anomalyFactory)
		val trainData = MutableDataset(ListDataSource(trainingDataExamples, anomalyFactory, trainingProvenance))

		val testingProvenance = SimpleDataSourceProvenance("Anomaly testing data", anomalyFactory)
		val testingData = MutableDataset(ListDataSource(testingDataExamples, anomalyFactory, testingProvenance))



		val params = SVMParameters(SVMAnomalyType(SVMAnomalyType.SVMMode.ONE_CLASS), KernelType.RBF).apply {
			gamma = 1.0
			setNu(0.1)
		}

		val trainer = LibSVMAnomalyTrainer(params)

		val startTime = System.currentTimeMillis();
		val model = trainer.train(trainData);
		val endTime = System.currentTimeMillis();
		println();
		println("Training took " + Util.formatDuration(startTime,endTime))
		println(">>>> number of supported vectors: ${(model as LibSVMAnomalyModel).numberOfSupportVectors}")

		var testEvaluation = AnomalyEvaluator().evaluate(model,testingData);
		println(testEvaluation.toString());
		println(testEvaluation.confusionString());
	}



}
