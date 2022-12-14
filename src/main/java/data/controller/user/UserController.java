package data.controller.user;

import data.domain.user.*;
import data.service.user.CouponService;
import data.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final CouponService couponService;

    //유저 정보 출력
    @GetMapping("/information")
    public User selectUser(int user_pk) {
        return userService.selectUser(user_pk);
    }
    //회원 정보 수정
    @PostMapping("/update")
    public void updateUser(@RequestBody User user) {
        userService.updateUser(user);
    }
    //회원가입 아이디 중복 체크
    @GetMapping("/idcheck")
    public int searchId(String u_id) {
        return userService.searchId(u_id); //아이디가 있으면 1 반환, 없으면 0 반환
    }
    //회원가입
    @PostMapping("/insert")
    public void insertUser(@RequestBody User user) {
        userService.insertUser(user);
        couponService.insertJoinCoupon();
    }
    //중복 닉네임 확인
    @GetMapping("/checkNick")
    public int selectNickname(String u_nick) {
        return userService.selectNickname(u_nick);
    }
    //본인 인증
    @GetMapping("/sendSMS")
    public String sendSMS(@RequestParam String u_phone) {
        Random rnd  = new Random();
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<4; i++) {
            buffer.append(rnd.nextInt(10));
        }
        String cerNum = buffer.toString();
//        System.out.println("수신자 번호 : " + u_phone);
//        System.out.println("인증번호 : " + cerNum);
        userService.certifiedPhoneNumber(u_phone, cerNum);
        return cerNum;
    }
    //비밀번호 변경
    @PostMapping("/updatepass")
    public void updatePass(@RequestBody User user) {
        userService.updatePass(user);
    }
    //아이디 찾기
    @GetMapping("/findid")
    public String selectFindId(String u_phone) {
        return userService.selectId(u_phone);
    }
    //비밀번호 찾기(아이디, 핸드폰 번호 넘겨서 둘 다 일치하는 레코드 있으면 1, 없으면 0 넘겨줌)
    @GetMapping("/findpass")
    public int selectFindPass(@RequestParam User user) {
        return userService.selectFindPass(user);
    }
    //회원 삭제(상태 변경)
    @GetMapping("/delete")
    public void deleteUser(String u_id) {
        userService.deleteUser(u_id);
    }
    //비밀번호 변경할 때 아이디 참조해서 기존 비밀번호 가져오기(기존 비밀번호와 일치하면 비밀번호 변경불가)
    @PostMapping("/selectpass")
    public boolean selectPass(@RequestBody User user) {
        return userService.selectPass(user);
    }

    // 영화 평점 등록
    @GetMapping("/insertReview")
    public void insertReview(String movie_pk, String user_pk, String revw_star,
                             @RequestParam(defaultValue = "") String revw_text) {
        userService.insertReview(movie_pk, user_pk, revw_star, revw_text);

    }

    // 영화 평점 수정
    @GetMapping("/updateReview")
    public void updateReview(String review_pk, String revw_star, String revw_text) {
        userService.updateReview(review_pk, revw_star, revw_star);
    }

    // 영화 평점 삭제
    @GetMapping("/deleteReview")
    public void deleteReview(String review_pk) {
        userService.deleteReview(review_pk);
    }

    // 평점 좋아요
    @PostMapping("/insertLikeRevw")
    public void insertLikeRevw(@RequestBody RevwClick revwClick) {
        userService.insertLikeRevw(revwClick);
    }

    // 평점 좋아요 취소
    @PostMapping("/deleteLikeRevw")
    public void deleteLikeRevw(@RequestBody RevwClick revwClick) {
        userService.deleteLikeRevw(revwClick);
    }

    // 평점 신고하기
    @PostMapping("/insertReport")
    public void insertReport(@RequestBody RevwClick revwClick) {userService.insertReport(revwClick);}

    // 평점 신고 취소하기
    @PostMapping("/deleteReport")
    public void deleteReport(@RequestBody RevwClick revwClick) {
        userService.deleteReport(revwClick);
    }

    // 평점 신고 유무 확인
    @PostMapping("/selectReportYorN")
    public boolean selectReportYorN(@RequestBody RevwClick revwClick) {
        return userService.selectReportYorN(revwClick);
        /* 값이 없으면 false 신고했으면 true 를 반환 */
    }

    // 영화 좋아요
    @PostMapping("/insertMWish")
    public void insertMWish(@RequestBody MWish mWish) {
        userService.isnertMWish(mWish);
    }

    // 영화 좋아요 취소
    @PostMapping("/deleteMWish")
    public void deleteMWish(@RequestBody MWish mWish) {
        userService.deleteMWish(mWish);
    }
}
