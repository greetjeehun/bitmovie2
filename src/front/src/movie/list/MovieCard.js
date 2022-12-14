import React, {useState} from 'react';
import {useNavigate} from "react-router-dom";
import {Rating} from "@mui/lab";

function MovieCard(props) {
    const data=props.movie_data;
    const navi=useNavigate();

    const [IsOn,setIsOn] = useState(false);

    return (
        <div className={'movie-card'}
             onMouseEnter={()=>setIsOn(true)}
             onMouseLeave={()=>setIsOn(false)}>
            <img className={`movie-card-poster`} alt={data.m_name} src={`https://image.tmdb.org/t/p/w500${data.m_photo.split(",")[0]}`}/>
            <div className={`movie-card-title ${IsOn? "on":"close"}`}
                 onClick={() => navi(`/movie/detail/${data.movie_pk}`)}>
                <div className={"movie-info-hover"}>
                    {data.m_info}
                </div>
                <div className={"movie-info-review-star"}>
                    평점 : &nbsp;&nbsp;<Rating name="star"
                                              // value={Number(data.revw_avgstar)}
                                              value={5}
                                              readOnly/>
                </div>
            </div>
        </div>
    );
}

export default MovieCard;