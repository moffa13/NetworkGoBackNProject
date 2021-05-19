package reso.examples.gobackn;

import reso.examples.gobackn.GBNCCProtocol.SENDER;

public class RenoCC {
	
	private float _cwnd = 1.0f;
	private boolean _slowStart = true;
	private int _lastACKSqNb = -1;
	private int _repeatedACK = 1;
	private static final int MAX_DUP_ACK = 3;
	private static final int DUP_ACK_CWND_DIVIDE = 2;
	private float _ssthresh = 100;
	private final GBNCCProtocol _proto;
	
	public RenoCC(GBNCCProtocol proto){
		_proto = proto;
	}
	
	public void timeout(){
		_proto.log(false, SENDER.BOTH, "slow start on");
		_proto.log(false, SENDER.BOTH, "Cwnd " + _cwnd + " => " + 1);
		_cwnd = 1;
		_slowStart = true;
	}
	
	public int getWindowSize(){
		return (int)_cwnd;
	}

	public void receiveACK(int sqNb) {
		int oldCwnd = getWindowSize(); 
		
		if(sqNb == _lastACKSqNb){ // Duplicate ACK
			_repeatedACK++;
			if(_repeatedACK >= MAX_DUP_ACK){ // 3 duplicate ACK
				_proto.log(true, SENDER.RECEIVER, "3 duplicate ACK (" + sqNb + ")");
				_proto.sendPacket(sqNb + 1);
				_cwnd /= DUP_ACK_CWND_DIVIDE; // Divide cwnd by 2
				if(_cwnd < 1.0f) _cwnd = 1;
				_ssthresh = _cwnd; // threshold equals to half of the congestion window when loss occurs.
			}
		}else{ // Different ACK
			_lastACKSqNb = sqNb;
			_repeatedACK = 1;
			
			if(_slowStart){ 
				_cwnd++;
				if(_cwnd > _ssthresh){ // cwnd is now beyond the threshold, disable SS
					_slowStart = false;
					_proto.log(false, SENDER.BOTH, "slow start off");
				}
			}else{
				// cwnd + (MSS^2 / cwnd)
				_cwnd = (_cwnd + (float)(Math.pow(GBNCCProtocol.MSS, 2) / _cwnd));
			}
			
		}
		
		_proto.log(false, SENDER.BOTH, "Cwnd " + oldCwnd + " => " + _cwnd);

		if(oldCwnd != getWindowSize()) {
			if(oldCwnd > getWindowSize()){
				_proto.log(false, SENDER.SENDER, "Resizing sliding window");
			}
		}
		
		
	}
}
