package com.github.neuralnetworks.calculation;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.github.neuralnetworks.architecture.Connections;
import com.github.neuralnetworks.architecture.GraphConnections;
import com.github.neuralnetworks.architecture.Layer;
import com.github.neuralnetworks.architecture.Matrix;
import com.github.neuralnetworks.calculation.neuronfunctions.ActivationFunction;
import com.github.neuralnetworks.calculation.neuronfunctions.ConstantConnectionCalculator;
import com.github.neuralnetworks.util.UniqueList;

/**
 * Default implementation for Connection calculator
 * Each inbound connection is calculated separately and the results are combined in the "output" matrix
 * Biases are also added
 * After all the input functions are calculated there is a list of activation functions that can be applied to the result
 * This class differs from LayerCalculatorImpl in the fact that LayerCalculatorImpl traverses the graph of layers, where ConnectionCalculatorImpl only deals with the connections passed as parameter
 * 
 * !!! Important !!!
 * The results of the calculations are represented as matrices (Matrix).
 * This is done, because it is assumed that implementations will provide a way for calculating many input results at once.
 * Each column of the matrix represents a single input. For example if the network is trained to classify MNIST images, each column of the input matrix will represent single MNIST image.
 */
public class ConnectionCalculatorImpl implements ConnectionCalculator {

    private static final long serialVersionUID = -5405654469496055017L;

    protected ConnectionCalculator inputFunction;

    /**
     * Activation functions
     */
    protected List<ActivationFunction> activationFunctions;

    public ConnectionCalculatorImpl(ConnectionCalculator inputFunction) {
	super();
	this.inputFunction = inputFunction;
    }

    @Override
    public void calculate(SortedMap<Connections, Matrix> connections, Matrix output, Layer targetLayer) {
	SortedMap<Connections, Matrix> notBias = new TreeMap<>();
	Map<GraphConnections, Float> bias = new TreeMap<>();

	for (Entry<Connections, Matrix> e : connections.entrySet()) {
	    Connections c = e.getKey();
	    Matrix input = e.getValue();
	    // bias layer scenarios
	    if (c instanceof GraphConnections && c.getInputLayer().getConnectionCalculator() instanceof ConstantConnectionCalculator) {
		ConstantConnectionCalculator cc = (ConstantConnectionCalculator) c.getInputLayer().getConnectionCalculator();
		bias.put((GraphConnections) c, cc.getValue());
	    } else {
		notBias.put(c, input);
	    }
	}

	calculateBias(bias, output);

	if (notBias.size() > 0) {
	    inputFunction.calculate(notBias, output, targetLayer);
	}

	if (activationFunctions != null) {
	    for (ActivationFunction f : activationFunctions) {
		f.value(output);
	    }
	}
    }

    protected void calculateBias(Map<GraphConnections, Float> bias, Matrix output) {
	if (bias.size() > 0) {
	    float[] out = output.getElements();
	    for (int i = 0; i < out.length; i++) {
		for (Entry<GraphConnections, Float> e : bias.entrySet()) {
		    out[i] += e.getKey().getConnectionGraph().getElements()[i / output.getColumns()] * e.getValue();
		}
	    }
	}
    }

    public void addActivationFunction(ActivationFunction activationFunction) {
	if (activationFunctions == null) {
	    activationFunctions = new UniqueList<>();
	}

	activationFunctions.add(activationFunction);
    }

    public void removeActivationFunction(ActivationFunction activationFunction) {
	if (activationFunctions != null) {
	    activationFunctions.remove(activationFunction);
	}
    }
}
