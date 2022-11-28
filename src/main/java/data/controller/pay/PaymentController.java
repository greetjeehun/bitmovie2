package data.controller.pay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import data.domain.pay.Booking;
import data.domain.pay.Payment;
import data.domain.pay.request.PaymentConfimDto;
import data.domain.user.Coupon;

import data.domain.user.MyPage;
import data.domain.user.User;
import data.service.api.IamportService;
import data.service.pay.BookingService;
import data.service.pay.PaymentService;
import data.service.user.CouponService;
import data.service.user.MyPageService;
import data.service.user.PointService;
import lombok.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;

    private final IamportService iamportService;

    private final MyPageService myPageService;

    private final BookingService bookingService;

    private final PointService pointService;

    private final CouponService couponService;


    @GetMapping("/confirm")
    public ResponseEntity confirm(@Valid int scrtime_pk, @Valid String seat_num){
        PaymentConfimDto paymentConfimDto = new PaymentConfimDto(scrtime_pk,seat_num);
        String result = bookingService.reservedSeatCheck(paymentConfimDto);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    //결제 성공 후 호출 메서드
    @PostMapping("/complete")
    public ResponseEntity<String> complete(@RequestBody ObjectNode object_node) throws IOException {

        // JackSon 라이브러리 사용
        ObjectMapper mapper =new ObjectMapper().registerModule(new JavaTimeModule());

        //ObjectNode 객체를 ObjectMapper로 읽어 자바객체로 인스턴스 선언.
        Payment payment = mapper.treeToValue(object_node.get("payment"),Payment.class);
        Booking booking = mapper.treeToValue(object_node.get("booking"),Booking.class);


        System.out.println("user_pk : "+ payment.getUser_pk());

        //일시가 9시간전으로 변환되는 오류때문에 9시간 추가.
        payment.setPay_date(payment.getPay_date().plusHours(9));
        booking.setBook_issu_date(booking.getBook_issu_date().plusHours(9));

        //1. 아임포트 토큰 생성
        String token = iamportService.getToken();
        //System.out.println("토큰 : " + token);

        //2. 토큰으로 결제 완료된 주문 정보 호출하여 취소시 사용할 amount 선언
        Map<String,Object> map = iamportService.paymentInfo(payment.getImp_uid(), token);
        int amount = (int)map.get("amount");
        System.out.println("아임포트 amount : " + amount);

        try {
            int my_point = pointService.selectPoint(payment.getUser_pk());
            int used_point = payment.getPay_use_point();

            //3. 사용한 포인트 유효성 검사
            if (my_point < used_point) {
                iamportService.paymentCancel(token, payment.getImp_uid(), amount, "사용 가능 포인트 부족");
                return new ResponseEntity<String>("[결제 취소] 사용 가능한 포인트가 부족합니다.", HttpStatus.BAD_REQUEST);
            }

            String used_coupon = payment.getMycoupon_pk();
            System.out.println("사용한 쿠폰 : "+used_coupon);

            if(!used_coupon.equals("N")){
                //4-1. 사용한 쿠폰이 있을 경우 유효성 검사
                try {
                    Coupon my_coupon = couponService.selectCouponState(used_coupon);
                    //4-2. 사용한 쿠폰 유효성 검사
                    if(my_coupon.getC_use_state()==1){
                        //System.out.println(my_coupon.getC_use_state());
                        iamportService.paymentCancel(token, payment.getImp_uid(), amount,"사용되었거나 유효기간이 지난 쿠폰 사용");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("[결제 취소] 선택한 쿠폰은 이미 사용되었거나 유효기간이 지난 쿠폰입니다.");
                    }
                } catch (NullPointerException e){
                    System.out.println("DB에 없는 쿠폰 번호 요청");
                    iamportService.paymentCancel(token, payment.getImp_uid(), amount,"유효하지 않은 쿠폰 사용");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("[결제 취소] 선택한 쿠폰은 유효하지 않은 쿠폰입니다.");
                }
            }


            //6. 포인트 차감 및 쿠폰 사용 처리
            pointService.deductionPoint(payment);
            couponService.updateCouponByPayment(1,used_coupon);

            //6. payment(결제) 데이터 저장
            paymentService.insertPaymentData(payment);

            //7. booking(예매) 데이터 저장
            bookingService.insertBookingData(booking);

            //8. point(point_tb,user_tb) 적립
            pointService.accumulatePoint(payment);

            return new ResponseEntity<>("주문이 완료되었습니다", HttpStatus.OK);
        } catch (Exception e){
            iamportService.paymentCancel(token, payment.getImp_uid(),amount,"결제 예러");
            e.printStackTrace();
            return new ResponseEntity<>("[결제 취소] 결제 처리중 문제가 발생했습니다. 관리자에게 문의하세요.", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/select_payment")
    public Payment selectPaymentData(String payment_pk){
        return paymentService.selectPaymentData(payment_pk);
    }


    @GetMapping("/cancel_payment")
    public ResponseEntity<String> cancelPaymentRequest(@RequestParam int user,
                                                        @RequestParam int booking) throws IOException {

        //유저고유키와 예매 고유키로 결제 고유키 및 아임포트 결제번호 조회
        Payment payment = paymentService.selectPayByUserAndBookPK(user,booking);

        //아임포트 토큰 생성
        String token = iamportService.getToken();
        //System.out.println("토큰 : " + token);

        try{
            iamportService.paymentCancel(token,payment.getImp_uid(),payment.getPay_price(),"결제 취소 정상 처리");
        } catch (NullPointerException ne){
            ne.printStackTrace();
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유저 정보가 없습니다.");
        }

        //아임포트 결제 정상 취소 확인
        Map<String,Object> map = iamportService.paymentInfo(payment.getImp_uid(), token);
        String status = (String) map.get("status");
        //System.out.println("결제 취소 결과 : "+status);

        //아임포트 결제상태가 취소일 경우 데이터 처리
        if(status.equals("cancelled")){

            //결제 취소일자 업데이트
            paymentService.updatePayCnclDate(payment.getPayment_pk());
            System.out.println(payment.getPayment_pk());

            //예매내역 제거
            bookingService.deleteBookingData(booking);

            return ResponseEntity.ok("결제 취소가 정상적으로 처리되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("결제 취소가 정상적으로 이루어지지 않았습니다.");
        }

    }
    //쿠폰 조회
    @GetMapping("/coupon")
    public List<Coupon> selectCoupon (int user_pk) {
        return couponService.selectCoupon(user_pk);
    }

    //리뷰 메크로 처리 프로그램
//    @GetMapping("/review_macro")
//    public void reviewMacroProgram(){
//        paymentService.insertReviewMacro();
//    }
}
